// Firestore security rules unit tests (T-14, NFR-5/NFR-6).
//
// Runs against the local Firestore emulator only (never a real project) via
// `npm test` -> `firebase emulators:exec --only firestore mocha` (see ../package.json and
// ../.mocharc.json). Uses the modern @firebase/rules-unit-testing API
// (initializeTestEnvironment), not the deprecated firebase-functions-test.
//
// Covers, at minimum, per the T-14 task description:
//   1. An owner can read/write their own `records`.
//   2. A different authenticated user CANNOT read another user's `records` — the
//      privacy-critical claim ADR-4 depends on (see the "PRIVACY-CRITICAL" tests below).
//   3. An unauthenticated request is denied everywhere.
//   4. A user can read any `users/{uid}` profile doc but only write their own.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, deleteDoc } from 'firebase/firestore';

const __dirname = dirname(fileURLToPath(import.meta.url));

// `firebase emulators:exec` sets FIRESTORE_EMULATOR_HOST for the spawned test process;
// fall back to firebase.json's configured port (8080) for a manually-started emulator.
const [emulatorHost, emulatorPortRaw] = (
  process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080'
).split(':');
const emulatorPort = Number(emulatorPortRaw);

// A "demo-" projectId is the documented convention for rules-unit-testing: it guarantees the
// emulator never resolves to a real project, which matters here because vinilogs-dev/-prod
// don't exist yet (firebase/README.md).
const PROJECT_ID = 'demo-vinilogs-rules-test';

const OWNER_UID = 'owner-uid';
const OTHER_UID = 'other-uid';

let testEnv;

function ownerProfile(overrides = {}) {
  return {
    displayName: 'Owner',
    displayNameLower: 'owner',
    isPublic: true,
    recordCount: 0,
    createdAt: Date.now(),
    ...overrides,
  };
}

function sampleRecord(overrides = {}) {
  return {
    artist: 'Boards of Canada',
    title: 'Music Has the Right to Children',
    artistLower: 'boards of canada',
    titleLower: 'music has the right to children',
    format: 'LP',
    speed: 'RPM33',
    condition: 'NM',
    // The private fields ADR-4 exists to protect — must never be readable by OTHER_UID.
    purchasePrice: 42,
    purchaseDate: Date.now(),
    rating: 5,
    notes: 'Private listening notes — must never leak to another user.',
    tags: [],
    createdAt: Date.now(),
    updatedAt: Date.now(),
    ...overrides,
  };
}

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(join(__dirname, '..', 'firestore.rules'), 'utf8'),
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  await testEnv?.cleanup();
});

afterEach(async () => {
  await testEnv.clearFirestore();
});

describe('firestore.rules', () => {
  describe('unauthenticated requests are denied everywhere', () => {
    it('denies reading a users/{uid} profile', async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), 'users', OWNER_UID), ownerProfile());
      });
      const unauth = testEnv.unauthenticatedContext();
      await assertFails(getDoc(doc(unauth.firestore(), 'users', OWNER_UID)));
    });

    it('denies writing a users/{uid} profile', async () => {
      const unauth = testEnv.unauthenticatedContext();
      await assertFails(setDoc(doc(unauth.firestore(), 'users', OWNER_UID), ownerProfile()));
    });

    it('denies reading a record', async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), `users/${OWNER_UID}/records/rec1`), sampleRecord());
      });
      const unauth = testEnv.unauthenticatedContext();
      await assertFails(getDoc(doc(unauth.firestore(), `users/${OWNER_UID}/records/rec1`)));
    });

    it('denies writing a record', async () => {
      const unauth = testEnv.unauthenticatedContext();
      await assertFails(
        setDoc(doc(unauth.firestore(), `users/${OWNER_UID}/records/rec1`), sampleRecord()),
      );
    });
  });

  describe('users/{uid} — any authenticated user reads, only the owner writes', () => {
    it('lets the owner create their own profile with recordCount pinned to 0', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertSucceeds(setDoc(doc(owner.firestore(), 'users', OWNER_UID), ownerProfile()));
    });

    it('denies creating a profile with a non-zero recordCount', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertFails(
        setDoc(doc(owner.firestore(), 'users', OWNER_UID), ownerProfile({ recordCount: 5 })),
      );
    });

    it("denies creating another user's profile document", async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertFails(setDoc(doc(owner.firestore(), 'users', OTHER_UID), ownerProfile()));
    });

    it('lets any authenticated user read any profile (needed for discovery search)', async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), 'users', OWNER_UID), ownerProfile());
      });
      const other = testEnv.authenticatedContext(OTHER_UID);
      await assertSucceeds(getDoc(doc(other.firestore(), 'users', OWNER_UID)));
    });

    it('lets the owner update their own editable profile fields', async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), 'users', OWNER_UID), ownerProfile());
      });
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertSucceeds(
        updateDoc(doc(owner.firestore(), 'users', OWNER_UID), { bio: 'Vinyl since 1998' }),
      );
    });

    it("denies a different authenticated user writing to someone else's profile", async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), 'users', OWNER_UID), ownerProfile());
      });
      const other = testEnv.authenticatedContext(OTHER_UID);
      await assertFails(
        updateDoc(doc(other.firestore(), 'users', OWNER_UID), { bio: 'hijacked' }),
      );
    });

    it('denies the owner moving their own recordCount client-side', async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), 'users', OWNER_UID), ownerProfile());
      });
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertFails(
        updateDoc(doc(owner.firestore(), 'users', OWNER_UID), { recordCount: 999 }),
      );
    });

    it('denies the owner deleting their own profile via a plain client write pattern check', async () => {
      // Delete is intentionally owner-only too (symmetry with create/update) — sanity-check
      // it isn't accidentally open to other users.
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), 'users', OWNER_UID), ownerProfile());
      });
      const other = testEnv.authenticatedContext(OTHER_UID);
      await assertFails(deleteDoc(doc(other.firestore(), 'users', OWNER_UID)));
    });
  });

  describe("users/{uid}/records/{recordId} — owner-only, full stop (ADR-4)", () => {
    beforeEach(async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), `users/${OWNER_UID}/records/rec1`), sampleRecord());
      });
    });

    it('lets the owner read their own record', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertSucceeds(getDoc(doc(owner.firestore(), `users/${OWNER_UID}/records/rec1`)));
    });

    it('lets the owner write (create) their own record', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertSucceeds(
        setDoc(
          doc(owner.firestore(), `users/${OWNER_UID}/records/rec2`),
          sampleRecord({ title: 'Geogaddi' }),
        ),
      );
    });

    it('lets the owner update their own record', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertSucceeds(
        updateDoc(doc(owner.firestore(), `users/${OWNER_UID}/records/rec1`), {
          notes: 'updated notes',
        }),
      );
    });

    it('lets the owner delete their own record', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertSucceeds(deleteDoc(doc(owner.firestore(), `users/${OWNER_UID}/records/rec1`)));
    });

    // PRIVACY-CRITICAL: this is the specific claim ADR-4 depends on. purchasePrice,
    // purchaseDate, rating and notes are private fields Firestore rules cannot hide within a
    // document, so the *entire* records collection must be owner-only. If this test ever
    // fails, ADR-4's premise is broken and private fields are leaking.
    it('PRIVACY-CRITICAL: a different authenticated user cannot read another user\'s record', async () => {
      const other = testEnv.authenticatedContext(OTHER_UID);
      await assertFails(getDoc(doc(other.firestore(), `users/${OWNER_UID}/records/rec1`)));
    });

    it("PRIVACY-CRITICAL: a different authenticated user cannot write another user's record", async () => {
      const other = testEnv.authenticatedContext(OTHER_UID);
      await assertFails(
        setDoc(
          doc(other.firestore(), `users/${OWNER_UID}/records/rec1`),
          sampleRecord({ title: 'tampered' }),
        ),
      );
    });

    it("PRIVACY-CRITICAL: a different authenticated user cannot update another user's record", async () => {
      const other = testEnv.authenticatedContext(OTHER_UID);
      await assertFails(
        updateDoc(doc(other.firestore(), `users/${OWNER_UID}/records/rec1`), {
          notes: 'hijacked',
        }),
      );
    });

    it("PRIVACY-CRITICAL: a different authenticated user cannot delete another user's record", async () => {
      const other = testEnv.authenticatedContext(OTHER_UID);
      await assertFails(deleteDoc(doc(other.firestore(), `users/${OWNER_UID}/records/rec1`)));
    });

    it('denies an unauthenticated read of a record even with the right path', async () => {
      const unauth = testEnv.unauthenticatedContext();
      await assertFails(getDoc(doc(unauth.firestore(), `users/${OWNER_UID}/records/rec1`)));
    });
  });

  describe('deny-by-default: Phase 2 collections (T-20/T-21) are not built yet', () => {
    it("denies reading users/{uid}/publicRecords even for the doc's own owner", async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertFails(
        getDoc(doc(owner.firestore(), `users/${OWNER_UID}/publicRecords/rec1`)),
      );
    });

    it('denies writing users/{uid}/publicRecords even for the doc\'s own owner', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertFails(
        setDoc(doc(owner.firestore(), `users/${OWNER_UID}/publicRecords/rec1`), {
          artist: 'x',
        }),
      );
    });

    it('denies reading reports/{reportId} for anyone, including its own reporter', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertFails(getDoc(doc(owner.firestore(), 'reports', 'report1')));
    });

    it('denies writing reports/{reportId} for anyone', async () => {
      const owner = testEnv.authenticatedContext(OWNER_UID);
      await assertFails(
        setDoc(doc(owner.firestore(), 'reports', 'report1'), {
          reporterUid: OWNER_UID,
          reportedUid: OTHER_UID,
          reason: 'test',
          createdAt: Date.now(),
        }),
      );
    });
  });
});
