# Domain Pitfalls: Dread v1.1 Polish Features

**Domain:** Minecraft Horror Mod Enhancement (Fabric 1.21.x)
**Version:** v1.1 Enhancements
**Researched:** 2026-01-25
**Focus:** Adding crawl pose, attack prevention, dread texture improvements, intense cinematic effects, and real audio to existing Dread v1.0 mod

**Context:** v1.0 already handles client-server separation (proper source sets), sound channel management (priority system exists), shader compatibility (runtime detection), and entity rendering with GeckoLib. This document focuses on pitfalls specific to v1.1 polish features.

---

## Critical Pitfalls

Mistakes that cause crashes, multiplayer desync, or require major rewrites.

### Pitfall 1: Client-Server Animation Desync (Crawl Pose)

**What goes wrong:** Crawl pose forced on client but not synchronized with server, causing position desync, rubberbanding, and multiplayer inconsistencies. Other players see the affected player in wrong position or glitching between standing/crawling states.

**Why it happens:**
- Pose manipulation code runs only on logical client without packet-based synchronization
- Animation state changes without notifying server of player state
- Swimming animation reused for crawling without proper server-side state tracking
- v1.0's client-server separation exists but new pose code bypasses it

**Consequences:**
- **Multiplayer desync:** Other players see glitching/teleporting player
- **Server kicks:** "Moved wrongly" or position violation kicks
- **Attack hitboxes misaligned:** Visual player position differs from server hitbox
- **Dread entity targeting breaks:** Entity attacks wrong position
- **Immersion destroyed:** Other players laugh at glitchy player instead of feeling fear

**Prevention:**
- Use v1.0's existing client-server separation pattern (proper source sets)
- Use Fabric's networking API to sync pose state changes between client/server
- Track pose state in both logical client AND logical server:
  - Server: Authoritative pose state for gameplay
  - Client: Visual representation only
- Send packets when entering/exiting crawl pose (both C2S and S2C)
- Verify server acknowledges pose change before applying visual effects
- Test in dedicated server environment, not just singleplayer (which uses integrated server)

```java
// SERVER: Receive crawl pose request from client
ServerPlayNetworking.registerGlobalReceiver(CRAWL_POSE_PACKET, (server, player, handler, buf, responseSender) -> {
    boolean shouldCrawl = buf.readBoolean();
    server.execute(() -> {
        // Server-side validation
        if (!canEnterCrawlPose(player, shouldCrawl)) return;

        // Update server-side state
        setCrawlPoseState(player, shouldCrawl);

        // Sync to ALL clients (including sender)
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(viewer, new CrawlPoseSyncPacket(player.getUuid(), shouldCrawl));
        }
    });
});
```

**Detection:**
- Test with two clients connected to dedicated server (not singleplayer)
- Watch second client's view while first player enters crawl pose
- Log server-side player position vs client-side position using F3 debug
- Check for "moved wrongly" or "moved too quickly" messages in server logs
- Use Entity Desync Viewer mod during testing to visualize hitbox misalignment

**Related Features:** Crawl pose forcing

**Phase Impact:** Crawl Pose Milestone - Must implement networking from day one, retrofitting sync is complex and error-prone.

**Sources:**
- [Fabric Wiki: Side Tutorial](https://wiki.fabricmc.net/tutorial:side)
- [Fabric Documentation: Networking](https://docs.fabricmc.net/develop/networking)
- [Entity Desync Viewer mod](https://modrinth.com/mod/entity-desync-viewer)

---

### Pitfall 2: Attack Prevention Server-Only Execution

**What goes wrong:** Attack prevention logic runs only on server, allowing client-side attack animations and sounds to play even when attacks are blocked. Player sees weapon swing, hears attack sound, but deals no damage. This breaks immersion and causes confusion.

**Why it happens:**
- Using `LivingHurtEvent` or similar server-side-only events
- Canceling attack damage without canceling client-side attack action
- Not communicating block status back to client for UI/audio feedback
- Treating attack prevention as damage cancellation instead of attack cancellation

**Consequences:**
- **Player confusion:** "Why didn't my attack work? Is my game broken?"
- **Audio pollution:** Attack sounds play repeatedly with no effect, breaking horror atmosphere
- **Animation desync:** Weapon swings but nothing happens
- **Multiplayer inconsistency:** Attacker sees hit, victim sees nothing
- **Dread mechanic unclear:** Players don't understand why they can't fight back

**Prevention:**
- Cancel attack at packet level (Use Entity packet 0x02) BEFORE damage calculation
- Send client notification when attack is prevented for UI/audio feedback
- Suppress client-side attack animations/sounds when blocked
- Update attack cooldown indicator to show prevention state (red X overlay, etc.)
- Handle both melee AND projectile attacks (see Pitfall 11)
- Test with both single-target and AOE attacks

```java
// SERVER: Cancel attack at event level
ServerPlayNetworking.registerGlobalReceiver(ATTACK_ENTITY_PACKET, (server, player, handler, buf, responseSender) -> {
    UUID targetUuid = buf.readUuid();
    server.execute(() -> {
        // Check if attacks are prevented (e.g., during dread state)
        if (isAttackPrevented(player)) {
            // Send feedback to client
            ServerPlayNetworking.send(player, new AttackPreventedPacket());
            return; // Don't process attack
        }
        // Process normal attack
    });
});

// CLIENT: Handle attack prevention feedback
ClientPlayNetworking.registerGlobalReceiver(ATTACK_PREVENTED_PACKET, (client, handler, buf, responseSender) -> {
    client.execute(() -> {
        // Visual feedback
        showAttackPreventedIndicator();
        // Stop attack sound if playing
        stopAttackSounds();
        // Show UI message
        client.player.sendMessage(Text.literal("You cannot attack!").formatted(Formatting.RED));
    });
});
```

**Detection:**
- Enable attack prevention, then spam attack button
- Listen for attack sounds - they should NOT play when attacks are blocked
- Watch for weapon swing animation - should be suppressed or show special "prevented" animation
- Check combat log for damage vs visual feedback mismatch
- Test in multiplayer with observer watching attacker

**Related Features:** Attack prevention during dread states, crawl pose restrictions

**Phase Impact:** Attack Prevention Milestone - Packet-level cancellation must be designed upfront; event-based cancellation creates UX debt.

**Sources:**
- [Minecraft Forums: Stopping mobs from attacking players](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/modification-development/2351019-stopping-mobs-from-attacking-players)
- [Hypixel Forums: Disabling attack packet](https://hypixel.net/threads/need-help-disabling-attack-packet.5536697/)

---

### Pitfall 3: GeckoLib Animation Conflicts with Pose Override

**What goes wrong:** v1.0's GeckoLib entity animations break or glitch when player pose override is active. Dread entity animations freeze, play at wrong times, or cause visual artifacts like stretched limbs when player enters crawl pose.

**Why it happens:**
- GeckoLib uses its own animation controller that conflicts with vanilla pose manipulation
- Animation keyframes don't account for forced pose states
- Both GeckoLib (for entities) and pose override (for player) try to control rendering simultaneously
- v1.0 entity rendering assumptions break when player model changes
- Render tick calculations assume standard player model bounds

**Consequences:**
- **Dread entity animations freeze** when player enters crawl pose
- **Player model limbs stretch or distort** during pose transitions
- **Animation transitions become jarring** instead of smooth
- **Shader incompatibilities worsen:** Entity shadows, translucency glitches amplified
- **v1.0 entity rendering breaks:** Existing working entities start glitching

**Prevention:**
- Check GeckoLib animation state before applying pose override
- Pause or adjust GeckoLib animations during pose-forced states
- Use GeckoLib's event keyframes to coordinate with pose changes
- Test with v1.0 entities present while pose override active
- Test with shaders enabled (Iris/Oculus) as they amplify conflicts
- Ensure GeckoLib version is 5.4.2+ (latest 2026 version with shader fixes)
- Use v1.0's existing shader compatibility runtime detection

```java
// Coordinate GeckoLib animations with pose state
@Override
public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    controllers.add(new AnimationController<>(this, "controller", 0, state -> {
        // Check if nearby players are in special pose states
        List<PlayerEntity> nearbyPlayers = world.getPlayers();
        boolean anyPlayerCrawling = nearbyPlayers.stream()
            .anyMatch(player -> isPlayerCrawling(player));

        if (anyPlayerCrawling) {
            // Adjust animation to avoid conflicts
            return state.setAndContinue(CAUTIOUS_ANIMATION);
        }

        return state.setAndContinue(NORMAL_ANIMATION);
    }));
}
```

**Detection:**
- Trigger dread entity spawn while in crawl pose
- Watch for frozen animations or limb distortion on BOTH player and entity
- Enable shader pack (Complementary, BSL) and check for entity shadow glitches
- Monitor console for GeckoLib animation controller errors
- Test animation transitions: normal → crawl pose → normal (while entity present)
- Check existing v1.0 entities for new bugs after pose code added

**Related Features:** Crawl pose + existing GeckoLib entity rendering integration

**Phase Impact:** Crawl Pose Milestone - Must coordinate with v1.0 entity rendering from start, fixing after implementation requires animation rework.

**Sources:**
- [GeckoLib GitHub](https://github.com/bernie-g/geckolib)
- [Iris/Oculus & GeckoLib Compat mod](https://modrinth.com/mod/geckoanimfix)
- [GeckoLib 5.4.2 release (Jan 2026)](https://www.curseforge.com/minecraft/mc-mods/geckolib/files/all)

---

### Pitfall 4: Sound Channel Saturation (v1.1 Expansion)

**What goes wrong:** New cinematic audio and dread ambient sounds exceed Minecraft's sound channel limit (~28 channels), causing critical sounds to be dropped. Player might miss important audio cues (entity footsteps, attack warnings) while new cinematic/ambient sounds play.

**Why it happens:**
- Adding high-priority cinematic sounds without integrating into v1.0 priority system
- Playing looping dread ambient sounds that never release channels
- Not respecting existing v1.0 sound channel management
- Multiple sound sources (entities, new ambience, cinematics, v1.0 sounds) competing for channels
- Stacking new v1.1 sounds on top of v1.0 without channel budget planning

**Consequences:**
- **"Failed to create new sound handle" errors** in logs
- **"Maximum sound pool size 247 reached" errors**
- **Critical entity sounds silenced:** Dread entity footsteps don't play during cinematic moments
- **Horror atmosphere breaks:** Jump scare sound might not play if channels full
- **Performance degradation:** Sound system overload causes FPS drops
- **v1.0 sounds blocked by v1.1 sounds:** New features break existing working audio

**Prevention:**
- **Use v1.0's existing priority system** and extend it:
  - Priority 1 (highest): Jump scares, critical entity sounds (v1.0)
  - Priority 2: New cinematic audio (v1.1)
  - Priority 3: Dread entity sounds (v1.0)
  - Priority 4: New dread ambient sounds (v1.1)
  - Priority 5 (lowest): World ambient sounds
- Stop looping sounds when switching states (exit dread, end cinematic)
- Implement sound pooling with v1.1 expansion:
  - Max 1 cinematic audio at a time (v1.1)
  - Max 2 ambient loops (1 from v1.0, 1 from v1.1)
  - Remainder for entities and interactions
- Before playing new cinematic sound, stop lower-priority ambient sounds
- Monitor active sound channels with debug logging
- Use Audio Engine Tweaks patterns for sound scheduling
- Test with: multiple dread entities + cinematic effects + ambient sounds simultaneously

```java
// Extend v1.0 sound priority system for v1.1
public class SoundManager {
    private static final int MAX_AMBIENT_SOUNDS = 2;
    private static final int MAX_CINEMATIC_SOUNDS = 1;
    private List<SoundInstance> activeAmbient = new ArrayList<>();
    private SoundInstance activeCinematic = null;

    public void playCinematicSound(SoundEvent sound, Vec3d pos) {
        // Stop existing cinematic
        if (activeCinematic != null) {
            stopSound(activeCinematic);
        }

        // Reduce ambient sounds to make room
        while (activeAmbient.size() > 1) {
            stopSound(activeAmbient.remove(0)); // Stop oldest ambient
        }

        // Play new cinematic (highest priority)
        activeCinematic = world.playSound(null, pos, sound, SoundCategory.AMBIENT, 1.0f, 1.0f);
    }
}
```

**Detection:**
- Check logs for "sound handle" or "sound pool" errors during gameplay
- Count active sound sources during peak moments (F3 debug + sound debug)
- Verify critical sounds play during cinematic sequences:
  - Entity footsteps should still be audible
  - Jump scare sounds must never fail
- Test with: 3+ dread entities + cinematic camera shake + ambient sounds + background music
- Use audio debug overlay to visualize channel usage

**Related Features:** Cinematic audio (v1.1) + dread ambient sounds (v1.1) + real audio integration (v1.1)

**Phase Impact:** Cinematic Effects Milestone + Real Audio Milestone - Must integrate with v1.0 sound system from start, adding later risks breaking existing audio.

**Sources:**
- [Audio Engine Tweaks mod](https://modrinth.com/mod/audio-engine-tweaks)
- [Minecraft Forums: Sound channel limits](https://www.minecraftforum.net/forums/minecraft-java-edition/discussion/3183671-momentary-lack-of-sound-effects-when-breaking)

---

## Moderate Pitfalls

Mistakes that cause delays, visual bugs, or technical debt.

### Pitfall 5: Texture UV Mapping Breaks on Model Changes

**What goes wrong:** Dread entity texture UVs break when switching between animation states or when pose override changes player model bounds. Textures appear stretched, squashed, or misaligned on improved v1.1 textures.

**Why it happens:**
- UV coordinates designed for one model state (standing) don't translate to another (crawling)
- Automatic UV mapping in Blockbench creates overlapping UVs
- Texture resolution doesn't use proper multiples of 16px
- Converting between model types (generic vs entity) mangles UVs
- v1.1 texture improvements applied without testing against all animation states

**Consequences:**
- **Gray/pink textures during pose transitions** (repeating v1.0 mistake: "Gray textures that break immersion")
- **Texture stretching on limbs** during crawl animation
- **Dread entity texture glitches** during certain animations (regression from v1.0)
- **Need to remake textures from scratch** (wasted v1.1 improvement work)
- **Immersion destroyed:** Players see placeholder textures instead of improved visuals

**Prevention:**
- **Learn from v1.0 mistakes:** No gray textures, no invalid placeholders
- Design UV maps for all animation states upfront (standing, crawling, etc.)
- Use consistent UV scale across all model parts (avoid stretching)
- Stick to entity-appropriate texture resolutions: 64x64, 128x128, 256x256 (multiples of 16)
- Avoid Blockbench auto-UV for complex models; manual UV mapping gives control
- Test UV map with animated model in ALL states before creating final texture
- Preserve texture ratios when scaling (maintain 1:1, 2:1, or 4:1 aspect ratios)
- Never convert model types (generic ↔ entity) after UV mapping
- Test improved textures in crawl pose before considering them "done"

**Detection:**
- Preview texture on model in all animation states in Blockbench (standing, crawling, downed)
- Check for UV overlap warnings in Blockbench
- Load model in-game and cycle through all animations
- Look for texture seams, stretching, or misalignment
- Verify texture dimensions are power-of-2 and multiples of 16px
- Ensure NO gray textures appear (v1.0 lesson learned)

**Related Features:** Dread entity texture improvements (v1.1) + crawl pose model changes (v1.1)

**Phase Impact:** Dread Texture Milestone + Crawl Pose Milestone - UV mapping must account for both features simultaneously.

**Sources:**
- [Blockbench: Minecraft Style Guide](https://www.blockbench.net/wiki/guides/minecraft-style-guide/)
- [LinkedIn: Common UV mapping mistakes](https://www.linkedin.com/advice/0/what-common-uv-mapping-texturing-mistakes-how-fix-them)
- [Blockbench UV mapping issue](https://github.com/JannisX11/blockbench/issues/822)

---

### Pitfall 6: Cinematic Camera Motion Sickness

**What goes wrong:** Intense cinematic camera shake and effects are too strong, causing player nausea, discomfort, or disorientation. Players disable the mod instead of experiencing enhanced horror atmosphere.

**Why it happens:**
- Camera shake intensity too high or too frequent
- No easing/smoothing on camera transitions (abrupt snapping)
- Camera effects triggered at low framerates amplify jitter/stutter
- No player control/accessibility options to reduce intensity
- Focusing on "intense" without considering motion sensitivity

**Consequences:**
- **Player complaints about motion sickness:** Bad reviews, uninstalls
- **Mod gets disabled or blacklisted**
- **Negative reviews citing nausea:** Damages mod reputation
- **Ruins horror experience:** Players feel sick instead of scared
- **Accessibility problems:** Excludes motion-sensitive players entirely
- **Horror atmosphere destroyed:** Can't be scared if you're nauseous

**Prevention:**
- Use **exponential decay** for camera shake (start strong, fade smoothly)
- Apply **simplex noise** for natural-feeling shake (not random jitter)
- Limit shake duration (0.5-2 seconds max, NOT sustained shaking)
- Recommend performance mods (Sodium/Lithium) for high FPS in mod description
- **Add config options (CRITICAL for accessibility):**
  - Shake intensity slider (0-200%, default 100%)
  - Toggle to completely disable camera shake
  - Separate intensity for different shake types (damage, cinematic, dread)
- Test at 30fps, 60fps, and 144fps to ensure smooth at all framerates
- Follow Camera Overhaul mod patterns: well-documented config with intensity scaling
- Add accessibility note in config about motion sensitivity
- Default to moderate intensity, let players increase if desired

```java
// Cinematic shake with accessibility
public void applyCinematicShake(float baseIntensity) {
    // Read from config
    float intensityMultiplier = config.getShakeIntensity(); // 0.0 to 2.0
    boolean shakeEnabled = config.isShakeEnabled();

    if (!shakeEnabled || intensityMultiplier <= 0.0f) {
        return; // Respect accessibility settings
    }

    float actualIntensity = baseIntensity * intensityMultiplier;

    // Apply with exponential decay
    applyShakeWithDecay(actualIntensity, 2.0f /* duration */);
}
```

**Detection:**
- Test cinematic sequences at 30fps (simulate low-end hardware with FPS cap)
- Ask multiple people to test; some are more motion-sensitive than others
- Check camera movement feels smooth, not jerky/snappy
- Verify config options actually reduce/disable effects
- Test sustained sequences (10+ seconds) not just brief moments
- Monitor community feedback post-release for motion sickness complaints

**Related Features:** Intense cinematic camera effects (v1.1)

**Phase Impact:** Cinematic Effects Milestone - Accessibility features must be designed in from start; adding config options later is retrofitting band-aid.

**Sources:**
- [Camera Overhaul mod](https://modrinth.com/mod/cameraoverhaul)
- [Inertia! motion sickness reduction](https://modrinth.com/mod/inertia!)

---

### Pitfall 7: Audio Format Incompatibility

**What goes wrong:** Real audio files fail to load or cause errors due to wrong format, sample rate, channels, or encoding. Sounds are silent, crackling, or crash the game when v1.1 audio is added.

**Why it happens:**
- Using stereo files instead of mono (Minecraft uses mono for spatial audio)
- Wrong file format (MP3 instead of OGG Vorbis)
- Sample rate mismatch (48kHz instead of 44.1kHz standard)
- Bitrate too high causing performance issues
- Not following Minecraft's strict audio requirements

**Consequences:**
- **Silent audio files:** No error message, just no sound playing
- **Audio crackle or distortion** during playback
- **Performance degradation** with high-bitrate files (FPS drops during audio)
- **Spatial audio doesn't work:** Sounds don't move in 3D space, breaking immersion
- **Horror atmosphere fails:** Ambient dread sounds are critical, silence breaks experience

**Prevention:**
- **Use OGG Vorbis format ONLY** (.ogg files, NOT .mp3)
- **Convert to mono channel** (1 channel, not 2 for spatial audio)
- **Use 44.1kHz sample rate** (standard for Minecraft)
- **Keep bitrate reasonable:** 96-128kbps sufficient for horror ambience (higher wastes space)
- **Use proper sound categories:**
  - `AMBIENT` for atmosphere and environmental sounds
  - `HOSTILE` for entity sounds
  - `MASTER` for critical sounds
- Test spatial audio: move around sound source, verify volume/panning changes
- Check game log for audio loading errors on startup
- Use audio conversion tools with correct settings:

```bash
# Convert to Minecraft-compatible format using ffmpeg
ffmpeg -i input.mp3 -ac 1 -ar 44100 -b:a 128k output.ogg
# -ac 1: mono channel
# -ar 44100: 44.1kHz sample rate
# -b:a 128k: 128kbps bitrate
```

**Detection:**
- Load game and check logs for sound registration errors
- Play each new sound in-game to verify it works
- Walk toward/away from sound source to test spatial audio (stereo files won't spatialize)
- Monitor performance during audio playback (FPS drops indicate bitrate too high)
- Test with headphones to hear spatial positioning clearly

**Related Features:** Real audio implementation for dread ambience (v1.1)

**Phase Impact:** Real Audio Milestone - Audio format must be correct from import; converting after creation wastes time.

**Sources:**
- [Minecraft Wiki: Sounds](https://minecraft.wiki/w/Sounds)
- [MCreator: Sound categories](https://mcreator.net/forum/63120/what-do-different-sound-categories-mean)
- [Sound Physics Remastered audio issues](https://github.com/henkelmax/sound-physics-remastered/issues/219)

---

### Pitfall 8: Performance Impact from High-Res Textures

**What goes wrong:** Using high-resolution textures (512x512+) for dread entity improvements tanks FPS, especially with multiple entities or during cinematic sequences with many rendered elements.

**Why it happens:**
- Entity Texture Features (ETF) constantly switches textures, amplified by high resolution
- High entity density areas (multiple dread entities) load many high-res textures into VRAM
- VRAM consumption spikes, causing texture swapping lag and stuttering
- Shader packs multiply performance cost of high-res entity textures
- Focusing on visual quality without performance testing

**Consequences:**
- **FPS drops during critical horror moments** (ruins immersion worse than low-res textures)
- **Low-end systems become unplayable** (excludes player base)
- **Cinematic sequences stutter** instead of flowing smoothly (destroys atmosphere)
- **Shader users hit harder** than vanilla renderer users (common horror mod audience uses shaders)
- **Players disable v1.1 improvements** to restore performance (defeats purpose)

**Prevention:**
- **Use 64x64 or 128x128 for entity textures** (NOT 256x+ unless absolutely critical)
- Optimize texture file size with tools (keep under 50KB per texture file)
- Implement texture LOD (level of detail): lower-res versions at distance
- Test performance with 3+ entities on screen simultaneously
- Test with popular shader packs (Complementary, BSL, Sildur's)
- Warn users in mod description: recommend optimization mods for best experience
- Profile frame time during entity-heavy scenes (F3 debug screen)
- Consider artistic optimization: stylized lower-res can be more atmospheric than realistic high-res

**Detection:**
- Use F3 debug screen to monitor FPS during entity spawns
- Spawn 5+ dread entities, check FPS drop percentage (should not exceed 20% drop)
- Enable shader pack and repeat entity density test
- Check VRAM usage in task manager (should not exceed ~2GB for textures)
- Test on low-end system (integrated graphics, 8GB RAM)
- Monitor frame times, not just average FPS (stuttering ruins horror more than lower FPS)

**Related Features:** Dread entity texture improvements (v1.1)

**Phase Impact:** Dread Texture Milestone - Resolution choices affect artistic direction and performance; changing later requires texture recreation.

**Sources:**
- [Entity Texture Features performance impact](https://modern.cansoft.com/time-to-retire-etf-why-entity-texture-features-has-run-its-course/)
- [Minecraft Forums: Texture resolution FPS impact](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/resource-packs/resource-pack-discussion/2853251-does-going-lower-than-16x-actually-improve-fps)

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable without major refactoring.

### Pitfall 9: Invalid Placeholder Textures (v1.0 Repeat Prevention)

**What goes wrong:** Creating placeholder textures that are too small, wrong dimensions, or invalid formats causes black/magenta checkerboard patterns or gray textures instead of intended visuals. **This repeats v1.0's documented mistake.**

**Why it happens:**
- Forgetting v1.0 lesson: "Placeholder files that are too small/invalid"
- Forgetting v1.0 lesson: "Gray textures that break immersion"
- Using dimensions that aren't multiples of 16px
- Creating files over 256px on one axis
- Using uppercase or special characters in filenames
- Creating empty or corrupted placeholder files

**Consequences:**
- **Immersion-breaking magenta/black checkerboard** during development and potentially release
- **Gray textures** (explicitly called out in v1.0 issues to NEVER repeat)
- Texture loading errors in logs
- Wasted time debugging why textures won't load
- **Repeating known mistakes destroys credibility**

**Prevention (v1.0 Lessons Applied):**
- Set minimum placeholder size: 64x64 for entities, 16x16 for items
- Use proper dimensions: 64x64, 128x128, 256x256 (never 256x512, 300x300, or odd sizes)
- Follow naming conventions: lowercase, underscores only (dread_entity_texture.png, NOT DreadTexture.png)
- Create valid placeholder with basic color/pattern (not empty file, not 1x1 file)
- Check logs on startup for texture loading errors
- Never exceed 256px on any single axis without testing
- Use PNG format with proper compression
- Document v1.0 mistakes in team notes to prevent repetition

**Detection:**
- Check game logs for "invalid texture" or "dimensions exceed" errors
- Look for magenta/black checkerboard or gray textures in-game
- Verify texture files open correctly in image editor
- Confirm filenames are lowercase with underscores only
- Run texture validation before committing

**Related Features:** All texture work (dread entity improvements, pose-related textures)

**Phase Impact:** Dread Texture Milestone - Placeholder quality matters for development workflow.

**Sources:**
- [Minecraft Wiki: Missing textures](https://minecraft.wiki/w/Missing_textures_and_models)
- [MCreator: Invalid texture path](https://mcreator.net/forum/83997/invalid-texture-path)
- v1.0 Project Post-Mortem (documented issues)

---

### Pitfall 10: Pose Animation Conflicts with Other Mods

**What goes wrong:** Crawl pose animation conflicts with popular animation mods (Not Enough Animations, playerAnimator, Better Animations), causing animation glitches or forcing players to choose between mods.

**Why it happens:**
- Multiple mods trying to control player animation simultaneously
- Using swim animation for crawling conflicts with other mods' swim animation overrides
- Not using animation library API (playerAnimator) for compatibility
- Implementing custom animation system instead of leveraging existing libraries

**Consequences:**
- **Players must choose:** Dread mod OR animation mod (not both)
- **Reduced mod adoption** due to incompatibilities
- **Bug reports from users** with popular animation packs
- **Animation glitches:** Running animation plays during crawl, weird limb positions
- **Negative reviews citing incompatibility**

**Prevention:**
- **Integrate playerAnimator library** (117M+ downloads, compatibility-focused)
- Check for animation mod presence at runtime, defer to their system if found
- Use animation blend modes, NOT replacement modes
- Test with top animation mods during development:
  - Not Enough Animations
  - Better Animations resource pack
  - playerAnimator
- Document known incompatibilities in mod description if unavoidable
- Consider making playerAnimator a soft dependency (optional but recommended)

```java
// Check for playerAnimator and use it if available
if (FabricLoader.getInstance().isModLoaded("playeranimator")) {
    // Use playerAnimator API for compatibility
    registerAnimationWithPlayerAnimator();
} else {
    // Fallback to custom implementation
    registerCustomAnimation();
}
```

**Detection:**
- Install Not Enough Animations + Dread mod, test crawl pose
- Install Better Animations resource pack + Dread mod, test animations
- Check for visual glitches: multiple animations layered, limb positions wrong
- Test animation transitions: standing → crawling → swimming → standing
- Monitor community feedback for incompatibility reports

**Related Features:** Crawl pose forcing (v1.1)

**Phase Impact:** Crawl Pose Milestone - Integration approach must be decided early; refactoring animation system later is high-effort.

**Sources:**
- [playerAnimator mod](https://modrinth.com/mod/playeranimator)
- [Better Animations compatibility list](https://modrinth.com/resourcepack/better-animations)
- [Not Enough Animations](https://www.curseforge.com/minecraft/mc-mods/not-enough-animations)

---

### Pitfall 11: Attack Prevention Edge Case - Projectiles

**What goes wrong:** Attack prevention blocks melee attacks but forgets about projectiles (arrows, tridents, potions). Player can still damage entities they shouldn't be able to, breaking dread mechanics.

**Why it happens:**
- Event cancellation targets `AttackEntityEvent` (melee only)
- Projectile damage uses different event path: `ProjectileImpactEvent`
- Not considering indirect damage sources (explosions, potions, environmental)
- Testing only with sword/hand, not ranged weapons

**Consequences:**
- **Inconsistent attack prevention:** Melee blocked, ranged works (confusing UX)
- **Player confusion:** "Why can I shoot but not punch?"
- **Breaks intended dread mechanics:** Player can cheese encounters with bow
- **Multiplayer exploits:** Players discover ranged workaround, share it

**Prevention:**
- Cancel BOTH `AttackEntityEvent` AND `ProjectileImpactEvent`
- Handle indirect damage sources:
  - Splash potions (Harming, Poison)
  - Lingering effects (potions, area effects)
  - TNT and explosions
  - Environmental (lava buckets, fire)
- Test comprehensively with:
  - Bow and arrows
  - Crossbow
  - Trident (melee and thrown)
  - Splash potion of harming
  - Snowballs (non-damaging but still "attack")
  - Fishing rod (knockback)

```java
// Projectile attack prevention
ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
    if (source.isProjectile() && isAttackPrevented(source.getAttacker())) {
        // Block projectile damage
        return false;
    }
    return true; // Allow damage
});
```

**Detection:**
- Enable attack prevention
- Test melee attack with sword (should fail)
- Test bow/arrow (should also fail if prevention is comprehensive)
- Test splash potion of harming
- Test trident throw vs melee
- Test indirect damage (place lava, ignite TNT)

**Related Features:** Attack prevention during dread states (v1.1)

**Phase Impact:** Attack Prevention Milestone - Comprehensive event coverage needed from start; patching edge cases post-release is reactive.

**Sources:**
- [Minecraft Forums: Stopping mob attacks](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/modification-development/2351019-stopping-mobs-from-attacking-players)

---

### Pitfall 12: Audio Volume Imbalance

**What goes wrong:** New v1.1 cinematic audio or dread sounds are too loud/quiet relative to existing v1.0 game sounds and vanilla Minecraft sounds, forcing players to constantly adjust volume or miss important audio cues.

**Why it happens:**
- Not normalizing audio files to consistent volume level
- Not accounting for Minecraft's volume categories (master, ambient, hostile)
- Recording/adding audio without reference to existing game volumes
- Stacking multiple sounds without volume compensation
- Testing with headphones only (or speakers only) instead of both

**Consequences:**
- **Cinematic audio blows out eardrums:** Players scramble for volume control
- **Dread ambience too quiet to hear:** Subtle horror cues missed
- **Player has to manually balance volumes** in settings (poor UX)
- **Horror atmosphere ruined** by volume inconsistency (jumpscares too loud, ambience inaudible)
- **Negative reviews:** "Mod is too loud" or "Can't hear the sounds"

**Prevention:**
- **Normalize all audio to -3dB peak level** before importing to mod
- Test in-game with master volume at 50%, category volumes at 100%
- Compare new sounds to vanilla sounds at same distance for reference
- Compare to v1.0 sounds for consistency within mod
- Use proper sound categories:
  - `AMBIENT` for atmosphere (subject to ambient volume slider)
  - `HOSTILE` for entities (subject to hostile volume slider)
  - `MASTER` for critical sounds (subject only to master volume)
- Document recommended volume settings in mod description if adjustment needed
- Implement volume scaling in config for user fine-tuning
- **Test with BOTH headphones AND speakers** (different audio experiences)

```java
// Audio with volume scaling from config
public void playDreadAmbient(SoundEvent sound, Vec3d pos) {
    float baseVolume = 0.7f; // Start at 70% for ambient
    float volumeMultiplier = config.getAmbientVolumeMultiplier(); // User config
    float finalVolume = baseVolume * volumeMultiplier;

    world.playSound(null, pos, sound, SoundCategory.AMBIENT, finalVolume, 1.0f);
}
```

**Detection:**
- Play game with volume at normal level (50% master)
- Trigger cinematic audio, check if you need to adjust volume immediately
- Trigger dread ambient sounds, verify they're audible but not overpowering
- Ask playtesters about volume balance (don't rely on developer familiarity)
- Compare to vanilla cave sounds and Nether ambience for reference baseline
- Test with different audio output devices (headphones, speakers, surround)

**Related Features:** Real audio implementation (v1.1) + cinematic audio (v1.1)

**Phase Impact:** Real Audio Milestone + Cinematic Effects Milestone - Volume normalization should happen during audio creation, not post-integration.

**Sources:**
- [Tense Ambience mod configuration](https://modrinth.com/mod/tense-ambience)
- [MAtmos audio balancing update](https://www.curseforge.com/minecraft/mc-mods/matmos2)

---

## Phase-Specific Warnings (v1.1 Features)

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| **Crawl Pose Implementation** | Client-Server Animation Desync (Critical #1) | Implement Fabric networking sync BEFORE visual effects |
| | GeckoLib Conflicts (Critical #3) | Test with v1.0 entities early, coordinate animation controllers |
| | Mod Compatibility (Minor #10) | Integrate playerAnimator API for broad compatibility |
| | Texture UV Breaks (Moderate #5) | Design UVs for crawl state from start, test in Blockbench |
| **Attack Prevention** | Server-Only Execution (Critical #2) | Cancel at packet level, sync feedback to client |
| | Projectile Edge Case (Minor #11) | Handle both melee AND projectile events comprehensively |
| **Dread Texture Improvements** | UV Mapping Breaks (Moderate #5) | Manual UV mapping for all animation states, no auto-UV |
| | Invalid Placeholders (Minor #9) | Apply v1.0 learnings: valid dimensions, proper naming |
| | Performance Impact (Moderate #8) | Stick to 64x64 or 128x128, test with multiple entities + shaders |
| **Cinematic Effects** | Motion Sickness (Moderate #6) | Exponential decay, config options, test at low FPS |
| | Sound Channel Saturation (Critical #4) | Integrate with v1.0 priority system, stop looping sounds |
| **Real Audio Implementation** | Format Incompatibility (Moderate #7) | OGG mono 44.1kHz only, test spatial audio |
| | Volume Imbalance (Minor #12) | Normalize to -3dB, test with vanilla sounds for reference |
| | Sound Channel Saturation (Critical #4) | Respect v1.0 channel limits, integrate with priority system |

---

## Integration Pitfalls with Existing v1.0 System

### Existing v1.0 Strengths to Preserve

1. **Client-Server Separation (Proper Source Sets)**
   - **Risk:** New v1.1 features bypass separation, causing dedicated server crashes
   - **Prevention:** Use existing client/server module structure for all new code
   - **Test:** Deploy to dedicated server, not just singleplayer testing

2. **Sound Priority System**
   - **Risk:** New v1.1 sounds ignore priority levels, breaking v1.0 channel management
   - **Prevention:** Route ALL new audio through existing priority manager
   - **Test:** Trigger v1.0 + v1.1 sounds simultaneously, verify priority ordering

3. **Shader Compatibility (Runtime Detection)**
   - **Risk:** New rendering (pose, textures) breaks shader detection or conflicts with shaders
   - **Prevention:** Test new visuals (pose, improved textures) with shader packs enabled
   - **Test:** Enable Complementary/BSL shaders, verify no visual regressions

4. **GeckoLib Entity Rendering**
   - **Risk:** Pose overrides conflict with GeckoLib animations, breaking v1.0 entities
   - **Prevention:** Coordinate animation controllers, test entity rendering during pose changes
   - **Test:** Spawn v1.0 entities while in crawl pose, verify animations work correctly

### Known v1.0 Issues to NEVER Repeat

1. **Placeholder Textures (too small/invalid)**
   - **For v1.1:** Create valid 64x64 minimum placeholders from day one
   - **Verification:** Check logs on startup, zero texture loading errors

2. **Gray Textures (immersion-breaking)**
   - **For v1.1:** Use proper dimensions (multiples of 16px), valid file formats
   - **Verification:** All textures show color/pattern, never gray placeholders

3. **No regression on v1.0 fixes:**
   - v1.0 solved these problems; v1.1 must not reintroduce them
   - Test v1.0 features after adding v1.1 features to catch regressions

---

## v1.1-Specific Testing Checklist

Before milestone completion:

### Crawl Pose Milestone:
- [ ] Pose syncs correctly in dedicated server (2+ clients)
- [ ] No client-server position desync (no rubberbanding)
- [ ] v1.0 GeckoLib entities still animate correctly during crawl pose
- [ ] Texture UVs remain correct in crawl pose
- [ ] Compatible with playerAnimator (if installed)
- [ ] No gray textures during pose transitions

### Attack Prevention Milestone:
- [ ] Melee attacks blocked correctly
- [ ] Projectile attacks (bow, trident, potions) also blocked
- [ ] Client receives feedback (no ghost animations/sounds)
- [ ] Attack prevention syncs in multiplayer

### Dread Texture Milestone:
- [ ] Improved textures work in all animation states (standing, crawling, downed)
- [ ] No UV stretching or misalignment
- [ ] Performance acceptable with 5+ entities on screen
- [ ] Shader compatibility maintained (test with Complementary, BSL)
- [ ] No gray/placeholder textures in release build
- [ ] Texture dimensions are multiples of 16px

### Cinematic Effects Milestone:
- [ ] Camera shake has config options (intensity, enable/disable)
- [ ] No motion sickness at 30fps, 60fps, 144fps
- [ ] Sound channels don't saturate (cinematics + v1.0 sounds work together)
- [ ] v1.0 sound priority system respected
- [ ] Performance remains 60+ FPS during cinematic sequences

### Real Audio Milestone:
- [ ] All audio files are OGG mono 44.1kHz
- [ ] Spatial audio works correctly (3D positioning)
- [ ] Volume balanced with vanilla sounds and v1.0 sounds
- [ ] Sound channel budget not exceeded (test with multiple sources)
- [ ] v1.0 priority system extended for new sounds
- [ ] Config options for volume adjustment

---

## Confidence Assessment

| Pitfall Category | Confidence Level | Source Quality |
|------------------|------------------|----------------|
| Client-Server Sync (v1.1 features) | HIGH | Fabric official docs + v1.0 patterns |
| Animation Conflicts (pose + GeckoLib) | MEDIUM | WebSearch + mod compatibility reports |
| Texture Issues (UV, formats) | HIGH | Blockbench docs + Minecraft Wiki + v1.0 lessons |
| Audio Format (OGG requirements) | HIGH | Minecraft Wiki + mod best practices |
| Performance Impact (textures, cinematics) | MEDIUM | Community reports + mod developer insights |
| Sound Channel Limits (v1.1 expansion) | MEDIUM | WebSearch + Audio Engine Tweaks + v1.0 system |
| Motion Sickness (accessibility) | MEDIUM | Camera mod best practices + accessibility standards |
| Attack Prevention (events) | MEDIUM | Fabric events + community patterns |

---

## Research Notes

**Most Critical for v1.1 Roadmap Planning:**
1. **Client-Server Animation Desync** (affects crawl pose milestone heavily, MUST sync from day one)
2. **Sound Channel Saturation** (affects both cinematic and audio milestones, must integrate with v1.0)
3. **GeckoLib Animation Conflicts** (affects integration with v1.0 entity system)

**Likely Needs Phase-Specific Research:**
- Crawl pose: Deep dive into playerAnimator integration for broad mod compatibility
- Cinematic effects: Accessibility testing with motion-sensitive users (external playtesters)
- Attack prevention: Comprehensive event coverage validation (all damage types)

**Low Risk / Standard Patterns:**
- Texture format compliance (well-documented, straightforward, v1.0 experience)
- Audio format conversion (tooling exists, clear specs, repeatable process)

**v1.0 Lessons Applied:**
- No gray textures (documented mistake, won't repeat)
- No invalid placeholders (documented mistake, won't repeat)
- Respect existing sound priority system (v1.0 strength, must preserve)
- Test with shaders (v1.0 already handles, must not break)

---

## Sources Summary

### Official Documentation (HIGH confidence)
- [Fabric Wiki: Side Tutorial](https://wiki.fabricmc.net/tutorial:side)
- [Fabric Documentation: Networking](https://docs.fabricmc.net/develop/networking)
- [Fabric Documentation: Events](https://docs.fabricmc.net/develop/events)
- [Blockbench: Minecraft Style Guide](https://www.blockbench.net/wiki/guides/minecraft-style-guide/)
- [Minecraft Wiki: Textures](https://minecraft.wiki/w/Textures)
- [Minecraft Wiki: Missing textures](https://minecraft.wiki/w/Missing_textures_and_models)

### Mod Compatibility & Best Practices (MEDIUM-HIGH confidence)
- [playerAnimator mod](https://modrinth.com/mod/playeranimator) - 117M+ downloads, animation compatibility library
- [GeckoLib GitHub](https://github.com/bernie-g/geckolib) - Animation engine documentation, v5.4.2 (Jan 2026)
- [Audio Engine Tweaks](https://modrinth.com/mod/audio-engine-tweaks) - Sound channel management patterns
- [Camera Overhaul](https://modrinth.com/mod/cameraoverhaul) - Cinematic camera best practices with config
- [Inertia! mod](https://modrinth.com/mod/inertia!) - Motion sickness reduction patterns
- [Entity Desync Viewer](https://modrinth.com/mod/entity-desync-viewer) - Debug tool for client-server position sync

### Community Knowledge (MEDIUM confidence)
- [Minecraft Forums: Animation conflicts](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1288552-aesthetic-animated-player-compatibility-and)
- [LinkedIn: UV mapping mistakes](https://www.linkedin.com/advice/0/what-common-uv-mapping-texturing-mistakes-how-fix-them)
- [Entity Texture Features performance analysis](https://modern.cansoft.com/time-to-retire-etf-why-entity-texture-features-has-run-its-course/)
- [Minecraft Forums: Texture resolution FPS impact](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/resource-packs/resource-pack-discussion/2853251-does-going-lower-than-16x-actually-improve-fps)
- [Tense Ambience mod](https://modrinth.com/mod/tense-ambience) - Horror atmosphere audio configuration
- [MAtmos audio balancing](https://www.curseforge.com/minecraft/mc-mods/matmos2) - Ambient sound volume management
