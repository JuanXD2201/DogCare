# Matriz de cumplimiento (resumen)

Matriz completa con evidencias: `DOCUMENTACION DogCare/12_MATRICES/Matriz_Maestra_DogCare.xlsx` (hoja *Cumplimiento académico*). Estados usados: REVISADO EN CÓDIGO, PRUEBA AUTOMATIZADA SUPERADA, CAPTURA OBTENIDA (render JVM), VERIFICADO EN DISPOSITIVO (sesión previa), PENDIENTE DE PRUEBA EN MÓVIL, BLOQUEADO.

| Punto de la guía | Módulo | Estado de verificación |
|---|---|---|
| P1 Calculadora y navegación | `CalculateDogAgeUseCase`, `AgeCalculatorScreen`, `DogCareApp` | Prueba automatizada; dispositivo (2026-10-28) |
| P2 Identidad visual | `ui/theme`, recursos | Revisado en código; renders |
| P3 Registro e inicio de sesión | `AuthRepository`, `AuthScreen`, `ProfileScreen` | Revisado en código; pendiente de prueba en móvil con dos cuentas |
| P4 Chat con IA | `AiAssistantRepository`, `EmergencyDetector` | Detector con prueba automatizada; respuesta real **pendiente** tras cambiar a `gemini-3.5-flash-lite` |
| P5 Veterinarias | `VetClinicRepository`, `VetMapScreen` | Prueba automatizada del parser; dispositivo con mapa lento |
| P6 Marketplace | `ProductRepository`, `StoreScreen` | Prueba automatizada; dispositivo |
| P7 Carrito | `CartRepository`, `OrderRepository` | Prueba automatizada; dispositivo |
| P8 Pasarela | `PaymentGatewayRepository` | **Simulación**; prueba automatizada de estados |
| P9 Chat entre usuarios | `UserChatRepository`, `UserChatScreen` | Reglas probadas en emulador (sesión previa); pendiente con dos cuentas |
| P10 Base de datos | `firestore.rules`, `docs/modelo_datos.md` | Revisado; 11 entidades mapeadas |
| P11 Pruebas | `src/test` | Ver informe de pruebas |
| P12 Documentación | `DOCUMENTACION DogCare/` | Generada; pendiente de revisión del docente |
