# Project Milestones: Dread

## v1.1 Polish & Immersion (Shipped: 2026-01-26)

**Delivered:** Replaced placeholder assets with terrifying horror textures and audio, added crawl pose with immersive effects, and enhanced death cinematic with extended grab animation and camera shake.

**Phases completed:** 5-8 (10 plans total)

**Key accomplishments:**

- Horror textures for all 3 Dread forms (BASE, EVOLVED, ELDRITCH) with emissive glowmasks
- 3-layer horror soundscape (ambient drone, proximity distortion, jump scare shriek)
- Attack prevention when downed (melee + projectile blocked server-side)
- Crawl pose system with EntityPose.SWIMMING and movement restrictions
- Blood vignette overlay, drip particles, and camera pitch limiting
- Extended 1.8s death grab animation with camera shake and FPS-adaptive compensation

**Stats:**

- 43 files created/modified
- 873 lines of Java added (3,757 total)
- 4 phases, 10 plans, ~35 tasks
- 3 days from milestone start to ship

**Git range:** `feat(05-01)` -> `docs(05)`

**Tech debt:** Missing grab_impact.ogg for death grab sound keyframe (LOW severity)

**What's next:** v2.0 Environmental Horror or community feedback integration

---

## v1.0 MVP (Shipped: 2026-01-25)

**Delivered:** Complete Minecraft horror mod with Cthulhu-style entity, turn-around jump scares, cinematic death sequences, and cooperative revival mechanics.

**Phases completed:** 1-4 (20 plans total)

**Key accomplishments:**

- Dread entity with GeckoLib animations, 3 form variants, and torch extinguishing
- Turn-around jump scare mechanics with Weeping Angel/SCP-173 stare freeze
- Mining and day-based escalation with 3:1 fake-out ratio for sustained psychological horror
- 4.5-second forced-perspective death cinematic with blur/vignette downed state
- 300-second revival window with crouch-to-revive and permanent spectator death
- GSON config system with shader compatibility detection (Iris/OptiFine)

**Stats:**

- 32 files created
- 2,953 lines of Java
- 4 phases, 20 plans, ~75 tasks
- 2 days from project start to ship

**Git range:** `feat(01-01)` -> `feat(04-03)`

**What's next:** v1.1 enhancements or v2.0 with environmental horror features

---
