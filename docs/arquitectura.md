# Arquitectura de DogCare

Documento vigente: `DOCUMENTACION DogCare/02_ARQUITECTURA_Y_DISENO/Arquitectura_Software_DogCare` (DOCX y PDF). Resumen fiel al código:

```mermaid
graph TD
    subgraph Presentacion[Presentación - Jetpack Compose y Material 3]
        A[DogCareApp - pestañas] --> B1[AgeCalculatorScreen]
        A --> B2[StoreScreen / CartScreen / CheckoutScreen]
        A --> B3[VetMapScreen]
        A --> B4[AiChatScreen]
        A --> B5[UserChatScreen]
        A --> B6[ProfileScreen / AuthScreen]
    end
    subgraph Dominio
        C1[CalculateDogAgeUseCase]
        C2[EmergencyDetector]
        C3[Modelos: Pet, Product, Order, CartItem, UserChat, VetClinic]
    end
    subgraph Datos[Datos - data/repository y data/util]
        D1[AuthRepository]
        D2[PetRepository]
        D3[ProductRepository]
        D4[OrderRepository / CartRepository]
        D5[VetClinicRepository]
        D6[AiAssistantRepository]
        D7[UserChatRepository]
        D8[PaymentGatewayRepository - simulado]
    end
    subgraph Externos
        E1[(Cloud Firestore)]
        E2[Firebase Authentication]
        E3[Firebase AI Logic - Gemini 3.x]
        E4[Overpass API - OSM]
        E5[Nominatim]
    end
    B1 --> C1
    B4 --> C2
    B1 --> D2
    B2 --> D3
    B2 --> D4
    B2 --> D8
    B3 --> D5
    B4 --> D6
    B5 --> D7
    B6 --> D1
    D1 --> E2
    D1 --> E1
    D2 --> E1
    D3 --> E1
    D4 --> E1
    D7 --> E1
    D5 --> E4
    D5 --> E5
    D6 --> E3
```

Notas: no hay ViewModels (el estado vive en los Composables y repositorios); no hay base local (Room se retiró por no usarse); la validación de precios y existencias ocurre en el cliente dentro de una transacción de Firestore.
