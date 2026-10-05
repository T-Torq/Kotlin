# Checklist-app

Den här appen är en enkel Android-checklist-app byggd med Jetpack Compose. Appen låter användaren skapa checklistor, öppna en checklista, lägga till/ändra/ta bort punkter och söka bland checklistor eller punkter.

Det finns ingen extern backend i produktion. Appen använder istället **Room-databasen** för att spara checklistor och punkter mellan appstart.

## Projektstruktur

- `app/src/main/java/com/example/checklist/MainActivity.kt`
  Huvudfilen för appens UI och logik.
- `app/src/main/java/com/example/checklist/data/ChecklistApiRepository.kt`
  Lagrings- och datalogik för checklistor och punkter via Room-databasen.
- `app/src/main/java/com/example/checklist/data/ChecklistDatabase.kt`
  Room-databaskonfiguration och singleton-instans.
- `app/src/main/java/com/example/checklist/data/ChecklistDao.kt`
  Data Access Object för Room-operationer (queries, inserts, updates, deletes).
- `app/src/main/java/com/example/checklist/data/ChecklistEntity.kt`
  Entity-klass för checklista i Room-databasen.
- `app/src/main/java/com/example/checklist/data/ChecklistItemEntity.kt`
  Entity-klass för checklistapunkter i Room-databasen.
- `app/src/main/java/com/example/checklist/ui/theme/`
  Temafiler för appens färer och färgtema.
- `app/src/main/AndroidManifest.xml`
  Appens manifest och inställningar för Android.

## Huvudfunktioner

### 1. Startsidan: lista över checklistor
Appens startsida visar en lista av checklistor.

Kod:
- `ChecklistHomeScreen()`
- `LazyColumn` med `items(filteredChecklists)`
- `ChecklistRow()`

Hur det fungerar:
- Appen laddar checklistor i `LaunchedEffect(Unit)` via `loadChecklists()`.
- `checklists` lagras i Compose-state.
- `filteredChecklists` filtreras med `checklistSearchQuery`.
- Klick på en rad öppnar checklistan i detaljvyn.
- Långtryck på en rad öppnar redigeringsdialogen för checklistans titel och färg.

### 2. Skapa ny checklista
Användaren kan skapa en ny checklista med namn och färg.

Kod:
- `showCreateDialog`
- `AlertDialog`
- `OutlinedTextField`
- `BorderColorPicker()`
- `repo.createChecklist(...)`

Hur det fungerar:
- När användaren trycker på plus-ikonen sätts `showCreateDialog = true`.
- Dialogen visar ett textfält för namn och en färgväljare.
- Efter att användaren tryckt "Skapa" anropas `repo.createChecklist(title)`.
- Den nya checklistan läggs till i listan och sparas lokalt.

### 3. Redigera checklista
En befintlig checklista kan byta namn och färg.

Kod:
- `checklistBeingEdited`
- `editText`
- `selectedBorderColorHex`
- `repo.updateChecklist(checklistId, title)`

Hur det fungerar:
- Långtryck på en checklista öppnar dialogen.
- `editText` fylls med nuvarande namn.
- Färg väljs via `BorderColorPicker()`.
- När användaren sparar skickas uppdateringen till repositoryt.
- UI:t uppdateras lokalt och det sparade värdet uppdateras i appens lokaldata.

### 4. Ta bort checklista
Checklistor kan tas bort.

Kod:
- delete-ikon i redigeringsdialogen
- `repo.deleteChecklist(checklistId)`

Hur det fungerar:
- I redigeringsdialogen finns en papperskorgsikon.
- När den trycks tas checklistan bort från den lokala snapshoten.
- Medföljande punkter i checklistan tas också bort.

### 5. Öppna detaljvy för en checklista
När användaren klickar på en checklista öppnas en ny vy med checklistans punkter.

Kod:
- `selectedChecklist`
- `ChecklistDetailScreen(...)`
- `loadChecklistItems(checklistId)`

Hur det fungerar:
- Valt `ChecklistApiModel` lagras i `selectedChecklist`.
- `loadChecklistItems(checklistId)` hämtar punkter via repositoryt.
- `checklistItems` uppdateras och renderas i detaljvyn.

### 6. Lista av punkter i en checklista
Varje punkt visas som en rad i detaljvyn.

Kod:
- `ChecklistItemRow()`
- `LazyColumn`

Hur det fungerar:
- `items` skickas till `ChecklistDetailScreen`.
- De filtreras mot `itemSearchQuery` om sökning är aktiv.
- Varje rad kan redigeras via långtryck.

### 7. Lägg till punkt
Användaren kan lägga till en ny punkt i en checklista.

Kod:
- `showCreateItemDialog`
- `newItemText`
- `repo.createChecklistItem(checklistId, name)`

Hur det fungerar:
- Plus-ikonen i detaljvyn öppnar en dialog.
- Dialogen visar textfält.
- När användaren sparar skapas ett nytt `ChecklistItemApiModel`.
- Det läggs till i `checklistItems` och sparas lokalt.

### 8. Redigera punkt
En punkt kan ändra namn.

Kod:
- `itemBeingEdited`
- `editText`
- `repo.updateChecklistItem(checklistId, itemId, name, isChecked)`

Hur det fungerar:
- Långtryck på en punkt öppnar dialogen.
- `editText` innehåller punktens nuvarande namn.
- vid "Spara" skickas uppdatering till repositoryt.

### 9. Ta bort punkt
En punkt kan tas bort från checklistan.

Kod:
- delete-ikon i redigeringsdialogen
- `repo.deleteChecklistItem(checklistId, itemId)`

Hur det fungerar:
- När användaren trycker på papperskorgsikonen tas punkten bort från den lokala state:n.
- Den tas även bort från lagring i snapshoten.

### 10. Sortering
Appen kan sortera checklistor efter namn, färg och datum.

Kod:
- `sortBy` och `sortOrder` state-variabler
- `getChecklists(sortBy, order)` i repositoryt
- Sortmeny med sorteringsalternativ
- ASC/DESC-knapp för att växla sorteringsriktning

Hur det fungerar:
- Klick på sorteringsikonen öppnar en meny med sorteringsalternativ.
- Alternativen är: Efter namn (A-Z), Efter namn (Z-A), Efter färg, Efter datum.
- ASC/DESC-knappen visar en pil upp (↑) för stigande och pil ned (↓) för fallande sortering.
- Klick på ASC/DESC-knappen växlar sorteringsriktningen och laddar om listan.
- Sorteringen sparas i state-variablerna `sortBy` och `sortOrder`.

### 11. Sökning
Appen har sökning i två nivåer:
- sökning efter checklistor
- sökning efter punkter i en checklista

Kod:
- `checklistSearchQuery`
- `itemSearchQuery`
- `filteredChecklists`
- `filteredItems`

Hur det fungerar:
- När användaren skriver i sökfältet uppdateras `checklistSearchQuery` eller `itemSearchQuery`.
- `filteredChecklists` och `filteredItems` beräknas med `.contains(..., ignoreCase = true)`.
- Endast matchande rader visas.

### 12. Färgväljare
Varje checklista kan ha en egen kantfärg.

Kod:
- `BorderColorPicker()`
- `selectedColorHex`
- `saveChecklistBorderColor(context, checklist)`
- `borderColorHex` sparas i Room-databasen

Hur det fungerar:
- En lista med färger renderas i en rad.
- Den valda färgen sparas i Room-databasen via `updateBorderColor()`.
- Detta gör att färgen blir kvar även om appen startas om.

## Repository-lagret och Room-databasen
Filer:
- `ChecklistApiRepository.kt` - Datalager som använder Room-databasen
- `ChecklistDatabase.kt` - Room-databaskonfiguration
- `ChecklistDao.kt` - Data Access Objects för Room-operationer
- `ChecklistEntity.kt` och `ChecklistItemEntity.kt` - Entity-klasser för databasen

ChecklistApiRepository är appens datalager. Det ansvarar för att spara och läsa checklistor och punkter från Room-databasen.

### Viktiga funktioner i repositoryt

#### `getChecklists(sortBy, order)`
Laddar checklistor från Room-databasen med valfri sortering (title, borderColor, createdAt).

#### `getChecklistItems(checklistId)`
Laddar punkter för en specifik checklista från Room-databasen.

#### `createChecklist(title)`
Skapar en ny checklista och sparar den i Room-databasen.

#### `createChecklistItem(checklistId, name)`
Skapar en ny punkt och sparar den i Room-databasen med koppling till checklistan.

#### `deleteChecklist(checklistId)`
Tar bort en checklista och dess tillhörande punkter från Room-databasen (CASCADE-delete).

#### `deleteChecklistItem(checklistId, itemId)`
Tar bort en punkt från Room-databasen.

#### `updateChecklist(checklistId, title)`
Uppdaterar checklistans titel i Room-databasen.

#### `updateChecklistItem(checklistId, itemId, name, isChecked)`
Uppdaterar en punkts namn eller status i Room-databasen.

#### `sortChecklists(checklists, sortBy, order)`
Sorterar checklista-listan efter valt fält och sorteringsriktning.

## State och UI-modeller

### `ChecklistApiModel`
Representerar en checklista i UI:t.

Fält:
- `id` - API-id för checklistan
- `title` - Checklistans namn
- `borderColorHex` - Kantfärg som hex-värde (t.ex. "#D3D3D3")

### `ChecklistItemApiModel`
Representerar en punkt i en checklista i UI:t.

Fält:
- `id` - API-id för punkten
- `checklistId` - Referens till checklistans API-id
- `name` - Punktens text
- `isChecked` - Om punkten är markerad

### Room-entiteter

#### `ChecklistEntity`
Databasklass för checklista i Room.

Fält:
- `id` - Primary key (auto-increment)
- `apiId` - Externt API-id
- `title` - Checklistans namn
- `borderColorHex` - Kantfärg som hex-värde
- `createdAt` - Tidpunkt för skapande
- `updatedAt` - Tidpunkt för senaste uppdatering

#### `ChecklistItemEntity`
Databasklass för checklistapunkt i Room.

Fält:
- `id` - Primary key (auto-increment)
- `checklistId` - Foreign key till ChecklistEntity
- `apiId` - Externt API-id
- `name` - Punktens text
- `isChecked` - Om punkten är markerad
- `createdAt` - Tidpunkt för skapande
- `updatedAt` - Tidpunkt för senaste uppdatering

## Flödet i appen

1. App startar.
2. Room-databasen initialiseras i `ChecklistDatabase.getDatabase()`.
3. `ChecklistHomeScreen()` byggs.
4. `LaunchedEffect(Unit)` kör `loadChecklists()`.
5. Checklistor hämtas från Room-databasen via repositoryt.
6. Checklistor sorteras enligt `sortBy` och `sortOrder`.
7. Användaren kan:
   - skapa checklista
   - öppna en checklista
   - söka
   - sortera
   - växla sorteringsriktning
   - redigera
   - ta bort
8. När checklistan öppnas hämtas punkter via `loadChecklistItems()` från Room-databasen.
9. Alla ändringar sparas omedelbar i Room-databasen via DAOs.

## Kort sammanfattning

Appen är en Compose-baserad checklist-app där UI:t byggs i `MainActivity.kt` och datahanteringen sker i `ChecklistApiRepository.kt`. Den använder **Room-databasen** för att lagra checklistor och punkter mellan appstarter. Appen stödjer sökning, sortering (efter namn, färg och datum), sorteringsriktning-växling (ASC/DESC), färgväljare för checklistor och möjlighet att skapa, uppdatera och ta bort checklistor och punkter.

## Nästa steg

Om du vill bygga vidare på appen kan du till exempel:
- lägga till checkbox-funktionalitet för punkter i UI:t
- lägga till möjlighet att markera punkter som klara
- lägga till kategorier eller taggar för checklistor
- skapa ett riktigt backend med API och authentication
- lägga till export/import-funktionalitet
- implementera dark mode

