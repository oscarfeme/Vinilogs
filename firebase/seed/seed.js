// Seeds the Firebase Emulator Suite (Auth + Firestore) with a handful of realistic users and
// records for manual dev testing. Run against emulators only — see ../README.md.
//
// This is deliberately small and hand-curated, unlike core:testing's 200-record fixture
// (T-07), which is for automated tests, not a human poking at the emulator UI.

import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

const PROJECT_ID = process.env.GCLOUD_PROJECT ?? "vinilogs-dev";

if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST) {
  console.error(
    "FIRESTORE_EMULATOR_HOST / FIREBASE_AUTH_EMULATOR_HOST are not set.\n" +
      "Run this via `firebase emulators:exec` (see firebase/README.md) so it never touches a real project.",
  );
  process.exit(1);
}

initializeApp({ projectId: PROJECT_ID });
const auth = getAuth();
const db = getFirestore();

const lower = (s) => s.toLowerCase();

const users = [
  {
    email: "amara@example.com",
    password: "password123",
    displayName: "Amara Okafor",
    bio: "Afrobeat and soul. Shop-hopping in Lagos and London.",
    location: "London, UK",
    isPublic: true,
    records: [
      { artist: "Fela Kuti", title: "Zombie", year: 1976, label: "Coconut", catalogNumber: "COC 001", format: "LP", speed: "RPM33", condition: "VG+", rating: 5, tags: ["afrobeat"] },
      { artist: "Tony Allen", title: "No Discrimination", year: 1979, label: "Fela's Kalakuta", catalogNumber: "FK 001", format: "LP", speed: "RPM33", condition: "NM", rating: 4, tags: ["afrobeat", "drums"] },
      { artist: "William Onyeabor", title: "Atomic Bomb", year: 1978, label: "Wilfilms", catalogNumber: "WIL 006", format: "LP", speed: "RPM33", condition: "VG", rating: 4, tags: ["synth-funk"] },
    ],
  },
  {
    email: "marcus@example.com",
    password: "password123",
    displayName: "Marcus Webb",
    bio: "Post-punk completionist. Doesn't lend records.",
    location: "Glasgow, UK",
    isPublic: true,
    records: [
      { artist: "Joy Division", title: "Unknown Pleasures", year: 1979, label: "Factory", catalogNumber: "FACT 10", format: "LP", speed: "RPM33", condition: "M", rating: 5, tags: ["post-punk"] },
      { artist: "Gang of Four", title: "Entertainment!", year: 1979, label: "EMI", catalogNumber: "EMC 3313", format: "LP", speed: "RPM33", condition: "VG+", rating: 5, tags: ["post-punk", "funk"] },
      { artist: "Wire", title: "Pink Flag", year: 1977, label: "Harvest", catalogNumber: "SHSP 4076", format: "LP", speed: "RPM33", condition: "VG", rating: 4, tags: ["post-punk"] },
      { artist: "The Fall", title: "Hex Enduction Hour", year: 1982, label: "Kamera", catalogNumber: "KAM 005", format: "LP", speed: "RPM33", condition: "G", rating: 3, notes: "Warped, plays fine side A only", tags: [] },
    ],
  },
  {
    email: "sofia@example.com",
    password: "password123",
    displayName: "Sofía Reyes",
    bio: null,
    location: null,
    isPublic: false,
    records: [
      { artist: "Silvana Estrada", title: "Marchita", year: 2022, label: "Glassnote", catalogNumber: "GLA 122", format: "LP", speed: "RPM33", condition: "M", rating: 5, tags: ["contemporary"] },
      { artist: "Cafe Tacvba", title: "Re", year: 1994, label: "WEA", catalogNumber: "WEA 8573", format: "LP", speed: "RPM33", condition: "VG+", rating: 4, tags: [] },
    ],
  },
];

async function seed() {
  for (const user of users) {
    const { records, email, password, ...profile } = user;

    const userRecord = await auth.createUser({ email, password, displayName: profile.displayName });
    const uid = userRecord.uid;

    await db.doc(`users/${uid}`).set({
      displayName: profile.displayName,
      displayNameLower: lower(profile.displayName),
      avatarUrl: null,
      bio: profile.bio,
      location: profile.location,
      isPublic: profile.isPublic,
      recordCount: records.length,
      createdAt: Timestamp.now(),
    });

    const batch = db.batch();
    for (const record of records) {
      const ref = db.collection(`users/${uid}/records`).doc();
      batch.set(ref, {
        artist: record.artist,
        title: record.title,
        artistLower: lower(record.artist),
        titleLower: lower(record.title),
        year: record.year ?? null,
        label: record.label ?? null,
        catalogNumber: record.catalogNumber ?? null,
        format: record.format,
        speed: record.speed,
        condition: record.condition,
        purchasePrice: null,
        purchaseDate: null,
        rating: record.rating ?? null,
        notes: record.notes ?? null,
        coverUrl: null,
        discogsId: null,
        tags: record.tags ?? [],
        createdAt: Timestamp.now(),
        updatedAt: Timestamp.now(),
      });
    }
    await batch.commit();

    console.log(`Seeded ${profile.displayName} <${email}> (${records.length} records, uid=${uid})`);
  }

  console.log("\nAll seed accounts use password: password123");
}

seed().catch((err) => {
  console.error(err);
  process.exit(1);
});
