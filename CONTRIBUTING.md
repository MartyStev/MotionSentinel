# Contributing to MotionSentinel

Thanks for your interest! Bug reports, translations and pull requests are welcome.

## Reporting bugs
Open an issue using the bug template. Include device model, Android version and app version. **Never paste your bot token, chat id, phone numbers or SMS contents.**

## Development
- JDK 17 and the Android SDK are required.
- Build: `./gradlew assembleDebug`; release build: `./gradlew assembleRelease` (unsigned unless signing env vars are set).
- Follow the official Kotlin code style (`kotlin.code.style=official`).

## Translations
Default (English) strings live in `app/src/main/res/values/strings.xml`. Add a new language as `values-<code>/strings.xml` containing every key; the Russian translation is in `values-ru`.

## Pull requests
1. Fork and create a branch from `main`.
2. Keep changes focused; describe what and why.
3. Make sure `./gradlew assembleDebug` passes (CI runs it on every PR).
4. Use conventional commit messages: `feat:`, `fix:`, `docs:`, `refactor:`, `chore:`.

By contributing you agree that your work is licensed under the MIT License.
