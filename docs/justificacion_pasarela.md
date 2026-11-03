# Pasarela de pagos: decisión y estado real

**Estado real:** DogCare **no integra ninguna pasarela real**. `PaymentGatewayRepository.processSandboxPayment` es una simulación local (espera 1,8 s y decide el resultado por la terminación de la tarjeta de prueba: `0002` o `9999` rechazado, `0004` pendiente, otra de 13 o más dígitos aprobado). No se realizan cobros ni se almacenan datos de tarjeta.

## Comparación de proveedores evaluados para una futura integración
| Criterio | Wompi | Mercado Pago | PayU |
|---|---|---|---|
| Medios locales (PSE, Nequi) | Nativo | Vía PSE | Vía PSE |
| Sandbox | Bueno, con tarjetas de prueba | Requiere cuentas de prueba | Más complejo |
| Integración móvil | Widget web / API REST | SDK Android | API REST |

Las comisiones varían y deben verificarse en la documentación vigente de cada proveedor antes de decidir.

## Producción
La confirmación del pago debe hacerla un servidor que valide la firma del proveedor (webhook) y actualice el pedido; el cliente solo inicia el pago y escucha el estado.
