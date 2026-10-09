# Brief for Claude: plan, critique, then build "Tacit", a keyboard-first Android launcher (v2)

## 0. How to read this document

This document has two authors:

1. **The owner**: the person who will own, publish and maintain this app. Their words are in Part A, copied exactly as written, including typos. Their words are the source of truth for *what* the app must be.
2. **The advisor**: an AI assistant the owner talked to before coming to you. The advisor's plan is in Part C. It is a proposal: detailed and reasoned, but open to challenge. Where the advisor guessed, assumed or made a judgement call, it says so. This is the advisor's second draft; the first draft was reviewed and its gaps folded in.

How to resolve conflicts:
- If the owner's verbatim words and the advisor's plan disagree, the owner wins. Point out the conflict so the owner can confirm.
- If you have a better idea than the advisor, say so, explain why, and propose it. Don't quietly swap it in. The owner wants to see the iteration between both of us and you.
- If a requirement is ambiguous, Part B lists the advisor's reading of it. Confirm or correct it, and ask the owner only when the answer would really change the design.
- Never weaken a hard requirement (speed, offline, F-Droid, no tracking, no comments, tests) to make a feature easier. Report the trade-off instead.

What I want from your first reply (no code yet): see Part F. Answer it in order. Do not write code until the owner approves the plan.

---

# PART A: The owner's words, verbatim

These are the owner's four messages, in order, unedited.

### Message 1 (the product)

> i want to do an android home app , it is heavily inspired by kiss launcher focusing on keyboard usage
> it's pretty fast and you have to make it the fastest ever we can do
> keyboard first you type and it gets it with even fuzzy and typos like typing gos can detect asmaa gouda
> it should search contacts and have a button to message , whatsapp , call which is done in their own app
> but the features i want and they are even doable or undoable which will determine whatever i will even do this app or not
> searching the full setting panels in android
> searching files as deeply as we can " the app shouldn't ask for root access though " and opening them within their respective app
> clipboard history with setting to save it for as long as you want " and some safety for sensitive info "
> using alias for any name / contact and is done in a tabualr form for batch aliasing for each category
> snippet support then copy we don't have to inject into apps
> a calculator that understand semantic words like raycast's
> you can search the web and using your browser to launch it
> minimalistic black and white but it should be very elegant yet for productivity first elegant logo and single word good mature iconic name
> support for widget is optional if you can do it right
> the app is just search bar  and optional clock date
> setting can tweaks things in users' favor but in genenral we are raycasting android
> everything is offline no internet access anywhere and this is aimed for fdroid so no anti features
> i don't want you to code anything this is planning so you tell me your opinion what can be realistically done " especially that i have to be as fast or faster than kiss launcher " and what can't be done
> also i want it to works well even if not the default home screen
> this is atomic not quippy or chatty on point won't pressure users to use it like other launchers quiet and ready when the users calls it no usage tracking no withholding apps

### Message 2 (handing off to you, with extra rules)

> give claude opus 5.5 the full app plan and add not adding code comments and checking with test that everything is good but is flutter okay for this or swift or native react a better option will flutter do it ?

### Message 3 (clarifying comments, asking about the environment)

> no code comments like the one in python , but can opus write and test kotlin in genspark ?
> first this enviroment right here can you build kotlin app in it ?

### Message 4 (asking for this document)

> one last time , give me your plan but this time not in short be explict about what you want and include what i wanted verbatim so opus can think and iterate on both of us for the best ideas you know opus is good for that

### What the advisor already told the owner, and the owner accepted without objection
- Framework: native Kotlin with classic Android Views. Not Flutter, React Native, Jetpack Compose or Swift. Swift is iOS-only, and iOS doesn't allow third-party launchers. Reasoning is in C2.
- The advisor's original workspace (a static-website builder) cannot compile Kotlin, run tests or produce an APK. The project must be built somewhere with a real shell, a JDK, the Android SDK and a device or emulator. See Part E.
- Speed benchmarks are only meaningful on a physical phone, comparing against KISS Launcher on the same device.

---

# PART B: The advisor's reading of each requirement, with the ambiguities

Each line quotes the owner, then gives the advisor's interpretation. **Confirm or correct each one in your first reply.**

**R1 "android home app … heavily inspired by kiss launcher focusing on keyboard usage"**
A real Android launcher (handles the HOME intent) whose whole interface is a search field with the soft keyboard already up. KISS Launcher is the reference for speed and philosophy, not for its features or looks.

**R2 "you have to make it the fastest ever we can do" / "as fast or faster than kiss launcher"**
Speed is the number-one requirement and decides architecture conflicts. "Fast" means four things: cold start (after Android has killed the app), hot start (summoning it again), time from a keystroke to drawn results, and zero dropped frames while typing. All measured on the same physical device against KISS, on a flagship and on a budget phone. Speed must be measured, not assumed.

**R3 "keyboard first you type and it gets it with even fuzzy and typos like typing gos can detect asmaa gouda"**
Fuzzy, typo-tolerant search.
- "gos" is NOT a subsequence of "asmaa gouda" (no "s" after the "o"). Plain fzf-style subsequence matching fails this exact example. It needs typo tolerance on word prefixes ("gos" is one edit from "gou").
- Ambiguity: the contact might be saved in Arabic script (أسماء جودة) while the owner types Latin letters. The advisor assumes this should work too, through transliteration.
- The phone keyboard's own autocorrect will fight this. It must be disabled on the search field (see C3).

**R4 "search contacts and have a button to message , whatsapp , call which is done in their own app"**
Each contact result has three action buttons, each opening the right external app (dialer, SMS app, WhatsApp). Tacit never places calls or sends messages itself.

**R5 "the features i want and they are even doable or undoable which will determine whatever i will even do this app or not"**
The owner needs an honest go/no-go per feature. Never overstate feasibility. The advisor's verdicts are in C1. Check them and correct any that are wrong for the current Android version.

**R6 "searching the full setting panels in android"**
Search every Settings screen. The system's own Settings search index is unreadable by normal apps, but C5.3 lists discovery techniques that should get close to full coverage of screens. Find anything better if it exists.

**R7 "searching files as deeply as we can … shouldn't ask for root access … opening them within their respective app"**
Deep file-name search across all user-accessible storage. No root. Open results in the default app for that type, or a chooser. "As deeply as we can" means find the real limit and reach it.

**R8 "clipboard history with setting to save it for as long as you want … some safety for sensitive info"**
Clipboard history, user-set retention (up to forever), protection for sensitive items. Android 10+ heavily restricts background clipboard reading. See C5.5.

**R9 "using alias for any name / contact and is done in a tabualr form for batch aliasing for each category"**
Aliases for any item (apps, contacts, settings, files, snippets), edited in a table, one table per category, so many items can be aliased quickly in one sitting.

**R10 "snippet support then copy we don't have to inject into apps"**
Saved text snippets. Selecting one copies it to the clipboard. No injection into other apps, so no accessibility service or custom keyboard.

**R11 "a calculator that understand semantic words like raycast's"**
An inline calculator understanding natural phrasing like Raycast's: "15% of 340", "5 km in miles", "time in tokyo". Fully offline.

**R12 "search the web and using your browser to launch it"**
Web search hands the query to the user's browser. Tacit itself has no internet.

**R13 "minimalistic black and white … very elegant yet for productivity first … elegant logo and single word good mature iconic name"**
Strictly monochrome, elegant, functional. One-word name, mature and iconic. A logo that works as a single-colour shape.

**R14 "support for widget is optional if you can do it right"**
Hosting third-party widgets is optional, and only if done to a high standard. Otherwise skip it. Separately, Tacit's *own* search-bar widget for other launchers is a different thing, and the advisor treats it as core (C6).

**R15 "the app is just search bar and optional clock date"**
The home screen shows only the search field plus an optional clock and date.

**R16 "setting can tweaks things in users' favor but in genenral we are raycasting android"**
Generous settings, strong defaults. The identity is "Raycast for Android".

**R17 "everything is offline no internet access anywhere … aimed for fdroid so no anti features"**
No INTERNET permission in the manifest at all. F-Droid inclusion with zero anti-feature labels: no tracking, no non-free dependencies, no non-free network services, no ads.

**R18 "also i want it to works well even if not the default home screen"**
Quick to summon and pleasant to use while another launcher is the home screen.

**R19 "atomic not quippy or chatty … won't pressure users … quiet and ready … no usage tracking no withholding apps"**
- No chatty copy, no onboarding, no nags, no engagement tricks, no system toasts triggered needlessly.
- No usage analytics. The advisor also takes this to mean the ranking doesn't learn from usage by default.
- Every installed app is always findable unless the user hides it.

**R20 "not adding code comments" + "no code comments like the one in python"**
- The advisor reads this as: no comments in the source at all, meaning the Kotlin equivalents of Python `#` comments (`//`, `/* */`) and of docstrings (KDoc `/** */`).
- Code explains itself through naming and structure; a static check enforces it; reasoning lives in `docs/decisions.md` (C10).
- Ambiguity: "like the one in python" could mean only explanatory line comments. The advisor chose the strict reading. Confirm with the owner. Until they answer, follow the strict reading.

**R21 "checking with test that everything is good"**
Comprehensive automated tests at every milestone. Report actual results you ran, never assumed passes. If a category can't run in your environment, say so and give the exact command for the owner.

**R22 "this is planning … i don't want you to code anything" (Message 1)**
That applied to the advisor. For you: planning and critique first (Part F). Code only after the owner approves.

---

# PART C: The advisor's plan (a proposal, so challenge it)

## C1. Feasibility verdicts

| Feature | Verdict | The catch |
|---|---|---|
| Fuzzy, typo-tolerant search ("gos" → Asmaa Gouda) | Fully doable | Custom multi-tier scorer; subsequence matching alone fails |
| Arabic contacts found by Latin typing | Doable on API 29+ | android.icu Transliterator; quality varies, so index both forms |
| Contacts with Call / Message / WhatsApp buttons | Fully doable | WhatsApp needs a number with a country code, or WhatsApp's own data row |
| Settings search | ~90% of screens on stock Android, less on heavy vendor skins | System Settings search *index* (individual toggles) is unreadable; screens are discoverable (C5.3) |
| Deep file search, no root | Mostly | Needs MANAGE_EXTERNAL_STORAGE ("All files access"), which F-Droid allows. Android/data, Android/obb and private storage unreachable |
| Clipboard history | Partial | Android 10+ lets only the focused app or the keyboard read the clipboard. Capture on focus; full background history needs hacks the advisor rejects |
| Sensitive-clip safety | Fully doable | None |
| Aliases in tables, per category, batch | Fully doable | None |
| Snippets, copy only | Fully doable | None |
| Semantic calculator | Mostly | No live currency rates offline |
| Web search through the browser | Fully doable | No search suggestions |
| Third-party widget hosting | Doable, fiddly | Late milestone, if at all |
| Tacit's own search-bar widget | Fully doable | None |
| Working well when not the default home | Mostly | App shortcuts only readable when Tacit is default home; more cold starts |
| Offline + F-Droid clean | Fully doable | No INTERNET permission is a selling point |
| Faster than KISS | Realistic | Only with C3, measured on a real device |

## C2. Tech stack, and why

- **Kotlin + Android Views (XML or programmatic), no Jetpack Compose.** Compose adds startup cost and a larger runtime. Flutter must start its engine before the first frame, adds ~15–20 MB, and still needs Kotlin bridges for every launcher feature (LauncherApps, AppWidgetHost, TileService, assist, contacts, files, clipboard, Settings intents). React Native adds a JS runtime on top. Those frameworks only replace the drawing layer, and a search field plus a list barely needs one.
- **Minimal dependencies.** AndroidX core, RecyclerView, profileinstaller, WorkManager (on-demand initialisation only), androidx.biometric (for the optional clipboard gate), Kotlin stdlib. appcompat only if justified, and if used, remove its EmojiCompat initialiser. Test-only: JUnit, Robolectric, Espresso, UI Automator, Macrobenchmark. No dependency-injection framework. Coroutines only if a benchmark shows no startup cost; a single HandlerThread or Executor is the default. Every dependency needs a written reason in README.md and an allowlist test.
- **minSdk 26** (advisor's choice; broad coverage). Settings panels and transliteration need API 29+ and degrade gracefully below. Raising minSdk to 29 is a valid alternative: weigh it and recommend (D7).
- **targetSdk / compileSdk:** latest stable. Note targetSdk 35+ forces edge-to-edge (C3).
- **Gradle version catalog.** R8 full mode and resource shrinking in release. Single module (plus the benchmark module), unless a split measurably helps.
- **Backup:** `allowBackup=false`; the JSON export (C9) is the backup path. The clipboard store and contacts index must never leave the device through Auto Backup.

## C3. Performance strategy

**Where the time really goes**
1. The keyboard slide-up animation (~100–250 ms) is the floor of perceived speed, not the search. Request the keyboard at the earliest moment the window has focus: WindowInsetsController.show(ime) on API 30+, SOFT_INPUT_STATE_ALWAYS_VISIBLE as fallback, search field focused at all times.
2. Icons. Decode once at fixed size into a memory cache backed by a disk cache. Load off the main thread with a placeholder glyph. Monochrome mode default, text-only mode available.
3. Cold start, whenever Android has killed the process. Staying warm matters as much as starting fast: a large heap gets killed sooner. Keep the heap small (file index memory-mapped, not heap objects).

**Architecture rules**
- All searchable data in memory as compact, pre-normalised primitive arrays.
- Persist the index as a versioned binary file, read in one pass or memory-mapped at startup. Never query content providers on the startup path; refresh in the background after the first frame.
- Normalise once when indexing: lowercase, strip diacritics, merge Arabic letter variants, Arabic-Indic/Persian digits → ASCII, split words, compute initials, transliterate. Per keystroke, normalise only the query.
- No allocations per keystroke: reused buffers, fixed-size top-K heap. No DiffUtil (allocates, O(n)); replace list contents directly or draw rows in one custom view.
- Apps, contacts, settings, aliases, snippets and calculator searched synchronously on the main thread (thousands of items, well under 1 ms). Files searched on one background thread cancelled per keystroke; instant results drawn first, file results appended. Skip file search for one-character queries.
- One RecyclerView, stable IDs, fixed-height rows. Consider a single custom-drawn view if it measurably helps (D1).
- **Stable top result:** rows already on screen never reorder unless scores really changed; Enter always acts on the current top row.
- Activity: launchMode singleTask; theme window background matches the app (no flash); move to background instead of finishing so it stays warm; window animations removable; excludeFromRecents configurable; onNewIntent (Home pressed while already home) clears the query and scrolls to top.
- **Startup traps:** disable WorkManager's automatic initialiser and any EmojiCompat initialiser (both run as ContentProviders at process start). Near-empty Application class.
- **Keyboard interaction:** disable the IME's suggestions and autocorrect on the field (KISS uses the textVisiblePassword trick; verify it still permits swipe typing and Arabic on Gboard, Samsung Keyboard, HeliBoard and FlorisBoard). Enter maps to the IME "go" action. The results list resizes to the keyboard through WindowInsets (ime type), never hidden behind it, on all four keyboards.
- Predictive back (Android 13+) opted in: Back clears the query, then minimises.
- Ship a **baseline profile** from the Macrobenchmark module with profileinstaller. Call reportFullyDrawn.

**Budgets** (enforced by benchmarks that fail when missed)

| Metric | Budget |
|---|---|
| Keystroke → non-file results drawn | < 8 ms p95, with 1,000 apps + 3,000 contacts |
| File results appended | < 30 ms p95, with a 200,000-file index |
| Hot start to first frame | < 50 ms |
| Cold start | Lower than KISS on the same device, flagship and budget phone |
| Dropped frames, 30-keystroke sequence | 0 |
| Release APK | < 3 MB unless a reason is written down |

**Study KISS Launcher's source** (GPL-3.0; github.com/Neamar/KISS) for why it's fast and where it isn't. **Study Kvaesitso** as a feature reference (files, calculator, units, contacts) and a speed cautionary tale (Compose). Copy code only where licences allow and attribution is given.

## C4. Search engine

**Scoring tiers.** A higher tier always outranks a lower tier.
1. Exact alias match
2. Exact full name
3. Prefix of the full name
4. Prefix of any word ("gou" → Asmaa **Gou**da)
5. Initials ("ag" → **A**smaa **G**ouda)
6. Letters in order with gaps, penalised for gaps, rewarded for word starts ("smgd")
7. Typo-tolerant word prefix: edit distance ≤1 for 3–4 character queries, ≤2 for 5+; adjacent swap counts as one edit; optional reduced cost for neighbouring keys; "gos" → "gou" → Asmaa Gouda
8. Ties: shorter names first, then stable alphabetical

**Multi-word queries** ("asm gou"): each query word must match some name word, in any order; score is the combination of per-word tiers (Claude proposes the formula, D2).

**Recognised inputs beyond names**
- A phone number → Call / Message / WhatsApp rows, no contact needed.
- A URL → Open in browser. An email address → mailto.
- A package name → the app.
- Calculator-shaped input → result row on top.

**Prefix grammar.** One documented, user-editable grammar so prefixes don't collide. Advisor's proposal, for Claude to finalise: `=` calculator only, `cb ` clipboard, `>` settings only, `@` contacts only, `/` files only, `ext:` and `in:` file filters, plus web-engine prefixes (`ddg `, `w `, `yt `). None required for the common case.

**Type priority.** Types: app, contact, setting, file, snippet, clipboard, calculator, web. User-reorderable.

**Ranking without tracking.** Default fixed, never learns. Aliases and pins are the user's steering wheel. Starred contacts rank higher only if the user enables "use starred as pins" (the user's own flag, not tracking). Optional "learn from launches" toggle, off by default: counts on device, decaying, clearable; when off, nothing is recorded. (Challenge this, D3.)

**Multilingual.** Arabic: merge أ/إ/آ → ا, ة → ه, ى → ي; strip harakat and tatweel. Transliteration on API 29+ (`Any-Latin; Latin-ASCII`), indexing both forms. Remove Latin diacritics. Phone numbers normalised to digits. RTL layout and Arabic UI strings from day one.

**Required test cases** (at least): gos → Asmaa Gouda; ag → Asmaa Gouda; asmaa → أسماء جودة; asm gou → Asmaa Gouda; wfi → Wi-Fi; blutooth → Bluetooth; yt → YouTube; exact alias beats prefix; empty query behaviour; a typed number yields action rows; 50+ more covering each tier and its ordering.

## C5. Features in detail

### C5.1 Apps
- LauncherApps API, all profiles including work profiles and Android 15 Private Space; profile callbacks; launch with the right UserHandle; locked-profile apps badged or hidden as the system does. `<queries>` for MAIN/LAUNCHER, no QUERY_ALL_PACKAGES.
- Incremental index updates on package added/removed/changed.
- Secondary actions: App info, notification settings, uninstall, alias, pin, hide.
- App shortcuts searchable only when Tacit is default home. Detect quietly, never warn.

### C5.2 Contacts (opt-in, READ_CONTACTS)
- Read **aggregated Contacts, not RawContacts**, or Google/WhatsApp/SIM copies duplicate. Index display names, nicknames, normalised numbers. ContentObserver keeps it fresh.
- **Call:** ACTION_DIAL by default (no permission). Opt-in direct call via ACTION_CALL + CALL_PHONE.
- **Message:** ACTION_SENDTO with smsto:.
- **WhatsApp:** ACTION_VIEW on the contact's `vnd.android.cursor.item/vnd.com.whatsapp.profile` data row when present (lands directly in the chat); otherwise wa.me/<number> restricted to the WhatsApp package, prepending the default country code. Button only when installed (`<queries>`). Support WhatsApp Business (`com.whatsapp.w4b`). Signal and Telegram optional, same pattern.
- Several numbers: act on the primary if marked, otherwise expand inline (owner to confirm, F.6).
- Contact photos off by default; initials glyph instead (monochrome, no decoding). Photos as a setting.
- Tacit never calls or sends anything itself.

### C5.3 Settings search
Coverage comes from five sources, deduplicated:
1. **Curated, localised catalogue** of every public Settings.ACTION_* screen with synonyms (hotspot/tethering, dark mode, night light…). Drop entries resolveActivity can't find.
2. **Runtime reflection over `android.provider.Settings` ACTION_* constants**, keeping those that resolve, so new API levels are covered automatically.
3. **Enumerate exported activities of the Settings app** (`com.android.settings` and vendor equivalents) with GET_ACTIVITIES: each enabled, exported, labelled activity is a candidate screen.
4. **Query ACTION_MAIN + category `com.android.settings.SHORTCUT`** (what powers "Settings shortcut" widgets; vendor builds often extend it).
5. **Per-app pages** for every app: "<app> info", "<app> notifications", "<app> storage", "<app> battery", plus **`ACTION_APPLICATION_PREFERENCES`** ("<app> settings" opens the app's own in-app settings) and `CATEGORY_NOTIFICATION_PREFERENCES` where they resolve.

Also: Settings panels (API 29+: internet, NFC, volume, Wi-Fi). Manufacturer deep pages behind a flag, failing quietly. Direct toggles: flashlight (CameraManager, no permission), brightness (opt-in WRITE_SETTINGS), Do Not Disturb (opt-in notification-policy access); Wi-Fi and Bluetooth open their panel, never pretend to toggle. Investigate a fallback row "Search in system Settings" that opens the system's Settings search where a resolvable intent exists (AOSP, Pixel, Samsung, Xiaomi), failing quietly.
**Honest limit:** the system's own search index (individual toggles inside screens) needs system privileges.

### C5.4 Files (opt-in)
- **Permissions:** API 30+ MANAGE_EXTERNAL_STORAGE; below 30 READ_EXTERNAL_STORAGE; without permission, MediaStore media only. **Middle path** for users who decline All files access: grant specific folders through ACTION_OPEN_DOCUMENT_TREE (persistable), walked with DocumentsContract directly (not DocumentFile). Android 11+ refuses the volume root and Download root in that picker.
- **Fast first index:** seed from the MediaStore Files table in one query, then walk the file system to fill gaps. Verify MediaStore completeness on real devices.
- **Index:** names, paths, extensions, sizes, modified dates across every shared volume incl. SD cards. Respect .nomedia. Hidden folders a toggle.
- **Unreachable:** Android/data, Android/obb, other apps' private storage, cloud files.
- **Structure:** compact arrays with a shared path-prefix table, persisted in binary form and **memory-mapped** (page cache, not heap; loaded lazily). Trigram or prefix index if a linear scan misses 30 ms. Background thread only.
- **Freshness:** MediaStore observer; FileObserver on user-chosen hot folders; periodic WorkManager rescan while charging and idle; manual "rescan".
- **Filters:** "pdf report", "ext:pdf", "in:downloads".
- **Opening:** ACTION_VIEW with FileProvider URI, MIME from extension, temporary read grant; chooser when no default. Secondary: share, open containing folder (ACTION_VIEW on a DocumentsContract URI with MIME `vnd.android.document/directory`, chooser fallback), copy path.
- Content search inside text files: deferred past v1, off by default.

### C5.5 Clipboard history (opt-in)
- **Platform wall:** Android 10+ lets only the focused app or the default keyboard read the clipboard.
- **Approach:** capture when Tacit's window gains focus, and anything copied inside Tacit. Users catch their latest copy every time they summon Tacit, but miss copies between summons. Say this honestly in the UI and README.
- **The Android 12+ "pasted from" toast.** getPrimaryClip() shows "Tacit pasted from <app>" each time. Fix to verify: on focus read only getPrimaryClipDescription() (no toast), compare its timestamp with the last stored one, and call getPrimaryClip() only when the clip is new. The toast then appears only when something new is saved, only for users who opted in.
- **Rejected:** accessibility-service focus tricks; shipping a keyboard. **Post-v1 "Advanced", off by default:** Shizuku-based background listener, evaluated for robustness and F-Droid fit (D5).
- **Safety:** respect ClipDescription.EXTRA_IS_SENSITIVE (API 33+). Detectors: card numbers (Luhn), IBANs (mod-97), 4–8 digit one-time codes, high-entropy tokens, private-key blocks. Per-category policy: never store, auto-delete after N seconds, or store with preview hidden. AES-GCM at rest, key in Android Keystore. FLAG_SECURE while clipboard or snippets are visible. Retention: forever, N days or N items. "Clear all" with one confirmation. App exclusion list where source is known. **Threat model stated honestly:** encryption defends against backup extraction and file reads, not against someone holding the unlocked phone; an optional biometric gate (androidx.biometric) in front of the clipboard view covers that.
- **Access:** "cb " prefix or the clipboard view. Enter copies the entry back.

### C5.6 Aliases
- One table per category (apps, contacts, settings, files, snippets). Columns: item name, then aliases (several, comma-separated). Inline editing, keyboard navigation, filtering.
- CSV import/export per category through the Storage Access Framework.
- Aliases sit in the in-memory index at the top tier.

### C5.7 Snippets
- Fields: trigger, title, body. Placeholders: {date}, {time}, {datetime}, {clipboard}, with optional formats.
- Enter copies the expanded text. Nothing injected into other apps.
- Same table editor and CSV import/export as aliases.

### C5.8 Calculator (hand-written grammar, no AI)
- BigDecimal with configurable precision. Decimal separators follow the locale. Arabic-Indic and Persian digits accepted.
- Arithmetic: + − × ÷, ^, parentheses, unary minus, factorial, modulo, sqrt, common functions, π, e.
- Natural language: "15% of 340", "340 + 15%", "half of 90", "twice 12", "5 plus 3 times 2".
- Units from offline tables: length, mass, volume, temperature, area, speed, data, time ("5 km in miles", "72f to c", "3gb in mb").
- Number bases: "255 in hex", "0xff in binary".
- Time zones via java.time: "5pm cairo in tokyo", "time in london". Needs a small bundled city → zone table from an openly licensed source, within the APK budget.
- Dates: "days until dec 25", "next friday + 3 weeks", "today - 2024-01-01".
- No currency conversion; optionally user-entered fixed rates.
- Result as the top row; Enter copies it. 150+ table-driven tests; malformed input never crashes. Match Raycast's feel.

### C5.9 Web search
- ACTION_VIEW with the query URL opens the default browser.
- User-editable engines with prefixes; a default engine when nothing matches or the user explicitly chooses web search.
- No suggestions, no INTERNET permission.

## C6. Ways to summon it (R18)

- **Default home:** HOME category. Back clears the query, then does nothing (or closes if not home).
- **Assistant:** ACTION_ASSIST, qualifying for the assistant role (minimal VoiceInteractionService only if required, no voice features). Verify the minimum on current Android (D8).
- **Quick Settings tile:** TileService, PendingIntent launch on Android 14+.
- **Tacit's own search-bar widget** for other launchers. Tap opens Tacit with the keyboard up.
- **Static app shortcut.**
- **Hardware keyboard:** Enter opens top result; arrows move; Tab cycles a row's actions; Esc clears, then closes.
- **Optional persistent notification**, off by default.
- **Behaviour:** every summon opens with an empty query and the keyboard up; closes after launching something (configurable); excluded from recents (configurable).
- **Discoverability without onboarding:** action buttons visible on rows themselves; one static "Keys" page in Settings listing shortcuts and prefixes. No pop-ups, no coach marks.

## C7. Privacy, F-Droid and quietness (R17, R19)

- No INTERNET permission; a test enforces it on the merged release manifest.
- No Google Play Services, Firebase, analytics, crash reporters or proprietary libraries. Allowlist test for runtime dependencies. Reproducible builds.
- **Offline crash log:** uncaught-exception handler writes the trace to app storage; Settings → "Share last crash" uses ACTION_SEND. No network, no library.
- Fastlane metadata, changelogs per version, semantic versioning. Licence: GPL-3.0 proposed. Weblate for community translations.
- Every permission requested only when its feature is turned on, never re-asked. With zero permissions the app is fully usable for apps, settings, calculator, web and snippets.
- No onboarding, tips, rating prompts, "did you know", promotions, update checks. No haptics, sounds or animations beyond the system's own, by default. Copy is short nouns and verbs.
- No app ever withheld. Hiding is explicit and reversible.
- **Explicitly out of scope** (so nothing creeps in): notification badges, double-tap-to-lock, icon packs (v1), gesture engine, app grid, folders, search history, usage statistics, update checks, crash-reporting services.

## C8. Identity (R13)

- **Working name: Tacit** ("understood without being said"). Alternatives: Kern, Sift, Cue. Propose better; check every candidate against F-Droid, Google Play and trademark searches.
- **Logo:** white rounded square with a single black text-cursor bar slightly off-centre; adaptive icon with monochrome layer for Android 13+ themed icons; vector drawable.
- **Look:** pure black (AMOLED), pure white, follow system; one typeface (system default unless a bundled open font is justified); hierarchy through spacing and weight only; app icons monochrome (own monochrome layer when present, else greyscale); solid surface by default, wallpaper optional; one layout that reflows for rotation, tablets and foldables.
- Home shows only the search field plus optional clock and date (off by default). Search bar at the bottom by default (owner to confirm, F.6).

## C9. Settings (R16)

- Theme, icon mode, wallpaper on/off, search bar position, clock and date, result count, type-priority order.
- Feature switches (contacts, files, clipboard, toggles), each triggering its own permission request.
- Fuzzy strictness (off, normal, loose), keyboard-neighbour typo cost, transliteration on/off.
- Web engines and prefixes, prefix grammar, calculator precision and unit system, default country code.
- Clipboard retention, sensitive policies, biometric gate, app exclusion list.
- Learn-from-launches (off), starred-as-pins (off), pins, hidden apps.
- "Keys" page. "Share last crash".
- Export and import of everything as one JSON file through the Storage Access Framework.

## C10. Testing, quality gates and documentation (R20, R21). All pass at every milestone.

- **Unit (JUnit):** scorer; normalisation and transliteration; calculator, units, time zones, dates; sensitive-data detectors; retention; CSV and JSON round-trips; binary index round-trip and version migration; clipboard-description timestamp dedupe.
- **Robolectric:** intent construction for every action (dial, SMS, WhatsApp, every Settings target, file opening, web); permission-granted and denied paths; Settings discovery dedupe.
- **Instrumented:** Espresso + Espresso-Intents (type → select → right intent fires); UI Automator for tile, widget, assistant; keyboard visible on open; results list not covered by the keyboard; FLAG_SECURE on the clipboard screen.
- **Macrobenchmark:** cold, warm, hot startup; frame timing for a 30-keystroke script; file-search latency on a synthetic 200k index; every C3 budget an assertion.
- **Fixtures:** a committed generated set of 1,000 fake apps and 3,000 contacts (Arabic, mixed-script, diacritics) so benchmarks are repeatable on any device.
- **Static:** Android Lint warnings-as-errors; ktlint; detekt with a rule failing the build on any comment (R20).
- **Manifest test:** no INTERNET; only expected opt-in permissions; no automatic WorkManager/EmojiCompat initialiser.
- **Fuzz:** random input to scorer and calculator never throws, stays within budget.
- **Docs, mandatory because the code has no comments:** `docs/decisions.md` (one entry per non-obvious choice: hand-initialised WorkManager, textVisiblePassword, memory-mapped index, description-only clipboard read…); `docs/limits.md` mirroring C12, linked from the F-Droid description; README with dependency reasons.
- **CI:** GitHub Actions running build, unit, Robolectric, lint, detekt, emulator instrumented tests. Benchmarks on the owner's phones.
- **Reporting:** pass/fail per suite, benchmark numbers against budgets, every test not run with the reason.

## C11. Milestones (stop and report after each)

- **M1 – Core speed:** search field with autocorrect disabled and inset handling, app search, full scorer with tests, binary index, monochrome icons, recognised inputs (number/URL/email), web search, basic calculator, HOME, ACTION_ASSIST, Quick Settings tile, search-bar widget, Macrobenchmark, baseline profile, no-INTERNET and no-initialiser tests, fixtures, decisions.md started, commands for the owner to compare against KISS.
- **M2 – People and system:** contacts with actions (aggregated, WhatsApp data row, Business); all five Settings discovery sources, panels, toggles.
- **M3 – Personalisation:** alias tables, snippets, prefix grammar, CSV and JSON import/export.
- **M4 – Files:** permission flow incl. folder-grant middle path, indexer, memory-mapped index, background search, freshness, FileProvider opening, open folder.
- **M5 – Clipboard:** description-first capture, detectors, encryption, retention, FLAG_SECURE, biometric gate.
- **M6 – Depth:** full calculator grammar, transliteration polish, hardware-keyboard polish, Private Space, accessibility pass (TalkBack, font scaling, contrast), RTL pass, "Keys" page.
- **M7 – Optional:** third-party widget hosting only if done right (R14), else document why skipped.
- **M8 – Release:** F-Droid metadata, reproducible-build check, limits.md, README, licence.

## C12. Known platform limits (state honestly, never work around with anti-features)

- The system Settings search index (toggles inside screens) can't be read; screens can be discovered.
- Android/data, Android/obb and private storage can't be searched without root.
- Clipboard history captures only what's on the clipboard when Tacit gains focus.
- App shortcuts need Tacit as the default home screen.
- No currency rates, no web suggestions, nothing that needs internet.

---

# PART D: Where I want you to push back, improve or decide

Think hard about each and give a recommendation with reasons:

1. **Beating KISS.** Is C3 the fastest realistic design? Weigh: single custom-drawn view vs RecyclerView; avoiding AppCompat entirely; memory-mapped index; pre-inflating the IME; process priority when not default home; heap size vs staying warm. Where would KISS still win, and how do we close the gap?
2. **The scorer.** Are the tiers and typo rules right? Two edits on short words floods results: propose thresholds that keep "gos" → Gouda without junk. Limit typo tolerance to word prefixes only? Propose the exact formula for multi-word queries ("asm gou").
3. **Ranking with no tracking.** Better than fixed ranking + aliases + pins + starred + optional local learning, fully respecting R19?
4. **Settings coverage.** With the five sources in C5.3, how high does coverage really go on AOSP, Pixel, Samsung, Xiaomi? Verify the "open system Settings search" fallback on vendor builds.
5. **Clipboard.** Any legitimate, non-hacky, F-Droid-acceptable way beyond on-focus? Evaluate Shizuku honestly. Confirm the description-first read avoids the Android 12+ toast. Otherwise design the best on-focus experience.
6. **Files.** Will MediaStore seeding plus walking reach 30 ms at 200k files? Which index structure? Is memory-mapping worth its complexity?
7. **minSdk 26 vs 29.** Pick one and justify.
8. **The assistant role.** Minimum needed to be selectable as default assistant on current Android with no voice features.
9. **Widget hosting.** Should it happen at all, given R14 and R15?
10. **Name and logo.** Better single-word names, checked for clashes.
11. **Keyboard autocorrect.** Confirm the best way to stop the IME fighting fuzzy search while keeping swipe typing and Arabic on Gboard, Samsung Keyboard, HeliBoard, FlorisBoard.
12. **Prefix grammar.** Finalise it (C4). Anything that collides with a real app or contact name a user might type?
13. **Anything the owner asked for that the advisor missed or diluted.** Re-read Part A line by line against Part C.
14. **Anything in the advisor's plan that contradicts the owner.** Flag it.

---

# PART E: Working rules and environment

- **Code style:** idiomatic Kotlin, no comments of any kind (R20, strict reading until confirmed), self-explanatory names, small functions. Reasoning goes in `docs/decisions.md`.
- **Environment you need:** Linux shell, JDK 17+, Android SDK command-line tools; internet during the build for Gradle and libraries (the *app* has none); ideally an emulator (KVM) for instrumented tests.
- **Honest reporting:** if you lack the shell or SDK, say so immediately rather than writing untested code. Benchmarks need the owner's physical phones (one flagship, one budget); write them, then give the exact Gradle commands.
- **Never claim a test passed unless you ran it.** Paste the real summary output.
- **Stop after each milestone** and wait for the owner's go-ahead.

---

# PART F: What your first reply must contain (no code)

1. A restatement of the product in five sentences or fewer.
2. For each R1–R22 in Part B: "agree", or your correction.
3. Your answers to every item in Part D (1–14).
4. A revised feasibility table if any C1 verdict changes.
5. Your proposed changes to the plan, each with reasons, marked as additions, removals or replacements.
6. The questions the owner must answer before M1, as few as possible, each with your recommended default. At minimum, confirm or replace the advisor's defaults on:
   - comments: strict "none at all" reading (R20)
   - search bar position: bottom
   - wallpaper behind the bar: off, solid surface
   - hidden apps: fully invisible in search, listed under Settings → Hidden
   - contacts with several numbers: primary if marked, else expand inline
   - minSdk: your pick from D7
   - name: Tacit or your proposal
   - licence: GPL-3.0
7. Confirmation of which environment you're running in and which test categories you can actually run there.

Only after the owner approves: start M1.
