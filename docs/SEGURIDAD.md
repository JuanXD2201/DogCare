# Seguridad de DogCare

Documento completo: `DOCUMENTACION DogCare/06_FIREBASE_Y_BASE_DE_DATOS/Documentacion_Firebase_y_Modelo_Datos`.

## Principios
1. Denegación por defecto en `firestore.rules`.
2. Validación de esquema por colección; el rol (`users.role`) no se asigna ni cambia desde la app.
3. Marketplace multivendedor: cada producto lleva `sellerUid`; solo su vendedor lo edita o borra. Un comprador solo descuenta stock ligado a su pedido (`PENDING` → `APPROVED`).
4. Carrito, pedidos, pagos y chats: solo el dueño o los participantes.
5. Los comprobantes de pago son inmutables.

## Limitaciones declaradas
- El pago es una **simulación**: el cliente decide el estado y las reglas no verifican `totalCop` contra los precios.
- `conversations.update` permite cambiar `participantUids` (recomendado: exigir que no cambie).
- `google-services.json` y `firebase_applet_config.xml` no se publican; restringir la clave de API por paquete y SHA-1.
