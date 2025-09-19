# Simulación de Sistemas TP3

## 👋 Introducción

Trabajo práctico para la materia de Simulación de Sistemas en el ITBA. Para choques de partículas en un recinto, simulando choques de partículas de gas.

### ❗ Requisitos

- Java 21+
- Maven

## 🏃 Ejecución
Primero se requiere compilar el paquete
```shell
mvn clean package
```

Finalmente se puede correr el programa por consola con el siguiente comando:
```bash
java {args} -cp {JAR_FILE} {MAIN_CLASS}
```
Con:
- `JAR_FILE` = target/SdS-TP3-1.0-SNAPSHOT.jar
- `MAIN_CLASS` = GasDiffusion
- `args` obligatorios listados en la siguiente sección

### 🛠️ Argumentos
Donde los argumentos son los siguientes:
- `N`: Cantidad de partículas
- `L`: Tamaño de la ranura entre cuadrado y rectángulo
- `epoch`: Cantidad de épocas máxima de la simulación
- `output`: Nombre opcional para el archivo de salida de la simulación

Ejemplo:
```shell
 java -DN=300 "-DL=0.03" -Depoch=500 -Doutput=out.txt -cp target/SdS-TP2-1.0-SNAPSHOT.jar OffLatticeSimulation
```
Nota: Los argumentos con punto pueden requerir estar en comillas dobles, como el caso de la velocidad.