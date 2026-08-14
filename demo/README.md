# Demo — JSON to Code Companion

Dos copias del mismo `sample.json`: una en la carpeta `java/`, otra en
`kotlin/` -- así se prueba la generación en ambos lenguajes usando el
mismo JSON de entrada, y de paso se prueba la resolución de paquete
(`com.example.demo`) en los dos casos.

## Pasos

1. Abrí `sample.json` (la copia en `java/` primero).
2. Seleccioná el objeto JSON completo (`Ctrl+A` adentro del archivo
   alcanza, es el único contenido).
3. Click derecho → **JSON to Code Companion → Generate Class from
   JSON**.
4. En el diálogo, poné un nombre de clase (ej. `Order`) → Enter.
5. Si pregunta el lenguaje (no debería, ya que estás parado en un
   archivo `.json` dentro de la carpeta `java/` -- confirmá si de
   verdad no pregunta o si sí lo hace), elegí **Java**.
6. Debería aparecer `Order.java` (y las clases anidadas que haga
   falta) en la misma carpeta, con el paquete `com.example.demo`
   correcto.
7. Repetí desde la copia en `kotlin/`, esta vez generando Kotlin.

## Qué mirar en el resultado

- **`shippingAddress`** → una clase anidada (`Address` o similar), no
  campos sueltos.
- **`tags`** → `List<String>` / `List<String>`.
- **`lineItems`** → `List<LineItem>` con una clase anidada generada
  para el shape `{sku, quantity, unitPrice}`.
- **`notifications`** (array vacío) → ¿qué tipo eligió? Es un caso
  límite real -- no hay forma honesta de inferir el tipo de un array
  vacío, así que esto es justamente lo que hay que revisar con
  atención (¿usa algo genérico tipo `Object`/`Any`, o falla con un
  aviso, en vez de inventar un tipo?).
- **`mixedShapeArray`** → los dos objetos tienen forma DISTINTA
  (`{kind, address}` vs `{kind, phoneNumber}`) -- otro caso límite real
  a revisar con atención, mismo criterio que arriba.
- **`totalCents`** (999999999999, no entra en un `int`) → ¿generó
  `long`, no `int`?
- **`couponCode`** (`null`) → ¿qué tipo le puso, siendo el único valor
  visto para ese campo?
- **`firstName`** → debería quedar `firstName`/`getFirstName()`, NO
  `firstname` (bug real ya encontrado y arreglado antes de esta
  sesión -- confirmar que se mantiene arreglado).

## Qué reportar

- ¿El archivo generado compila a simple vista (sin errores rojos)?
- ¿Los 2 casos límite (array vacío, array de forma mixta) se manejan
  de forma honesta (aviso o tipo genérico), no silenciosamente mal?
- ¿Java y Kotlin generan resultados equivalentes?
