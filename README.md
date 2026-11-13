# TrustBank Loans - Kotlin Android borrower app

This focused branch contains only `mobile/` and basic repository/CI support files. The `main` branch contains all three components together.

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

Read [the component guide](mobile/README.md). Build and deployment roots stay inside `mobile/`.

For the full system and safe folder synchronization, see [the layout guide on main](https://github.com/DenisJunior-3743/Client-server-BSE4/blob/main/docs/REPOSITORY-LAYOUT.md). These focused branches should transfer only their component folder back to main, rather than merging the whole reduced tree.
