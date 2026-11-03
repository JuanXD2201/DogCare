# Pruebas de DogCare

Informe vigente: `DOCUMENTACION DogCare/04_PRUEBAS_Y_CALIDAD/Informe_Pruebas_DogCare` (DOCX y PDF).

## Pruebas unitarias reales (`app/src/test/java/com/example`)
`CartCalculationsTest`, `DogAgeCalculatorTest`, `DogCareUnitTest`, `EmergencyDetectorTest`, `ExampleRobolectricTest`, `ExampleUnitTest`, `OrderMarketCorrectionTest`, `OverpassAndHaversineTest`, `PaymentStatusStateTest`, `ProductDogValidatorTest` y `ScreenshotsDocTest` (renders de documentación).

## Comando
```powershell
.\gradlew.bat testDebugUnitTest --console=plain --max-workers=1
```
En un equipo de 4 GB de RAM una compilación completa tarda ~20 minutos.

## Capturas de documentación (render JVM, no dispositivo)
```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.ScreenshotsDocTest" "-Proborazzi.test.record=true" --console=plain --max-workers=1
```
Salida: `app/src/test/screenshots/doc/`.

## Reglas de Firestore
`firestore.test.js` (23 pruebas) se ejecuta con el emulador de Firestore. Última ejecución registrada: sesión del 2026-10-28.
