# Fluxy Core

[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4-blue)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Fluxy Core** es un motor de flujos dinámicos y persistibles diseñado para orquestar lógica de negocio compleja mediante una arquitectura de tres niveles: **Task**, **Step** y **Flow**. Permite construir flujos reutilizables, bifurcaciones condicionales y trazabilidad completa de ejecución.

---

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Arquitectura](#-arquitectura)
  - [Niveles de Abstracción](#niveles-de-abstracción)
  - [Clases Intermediarias](#clases-intermediarias)
  - [Contexto de Ejecución](#contexto-de-ejecución)
- [Instalación](#-instalación)
- [Guía de Uso](#-guía-de-uso)
  - [1. Crear una Task](#1-crear-una-task)
  - [2. Configurar Steps](#2-configurar-steps)
  - [3. Definir un Flow](#3-definir-un-flow)
  - [4. Ejecutar el Flow](#4-ejecutar-el-flow)
- [Ejemplos Avanzados](#-ejemplos-avanzados)
  - [Flujo con Bifurcación Condicional](#flujo-con-bifurcación-condicional)
  - [Reutilización de Steps](#reutilización-de-steps)
- [API Reference](#-api-reference)
- [Roadmap](#-roadmap)
- [Contribución](#-contribución)
- [Licencia](#-licencia)

---

## 🚀 Características

- ✅ **Arquitectura en 3 niveles** (Task → Step → Flow) para máxima reutilización
- ✅ **Bifurcaciones dinámicas** mediante conexiones condicionales
- ✅ **Trazabilidad completa** de ejecución en `ExecutionContext`
- ✅ **Sistema de eventos** para monitoreo y auditoría
- ✅ **Operadores predefinidos** (EQ, NEQ, GT, LT, CONTAINS) extensibles
- ✅ **Flujos reutilizables** — misma tarea/step en múltiples contextos
- ✅ **Spring Boot ready** — autoconfigurable vía `spring.factories`
- 🔄 **Persistencia** (próximamente en librería separada)

---

## 🏗️ Arquitectura

### Niveles de Abstracción

Fluxy Core organiza la lógica de negocio en tres niveles de abstracción, siguiendo el principio de separación de responsabilidades:

```
┌─────────────────────────────────────────────────────────┐
│                      FluxyFlow                          │
│  (Definición del flujo de negocio completo)             │
│  - Conoce los steps vía FlowStep                        │
│  - Maneja bifurcaciones vía Connection                  │
└────────────────────┬────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
┌─────────▼─────────┐   ┌──────▼──────┐
│    FlowStep       │   │ Connection  │
│  (orden, estado)  │   │ (condiciones)│
└─────────┬─────────┘   └─────────────┘
          │
┌─────────▼──────────────────────────────────────────────┐
│                    FluxyStep                            │
│  (Unidad configurable)                                  │
│  - Conoce las tasks vía StepTask                        │
└────────────────────┬───────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
┌─────────▼─────────┐   ┌──────▼──────┐
│    StepTask       │   │             │
│  (orden, estado,  │   │   Múltiples │
│   resultado)      │   │   StepTasks │
└─────────┬─────────┘   └─────────────┘
          │
┌─────────▼──────────────────────────────────────────────┐
│                    FluxyTask                            │
│  (Implementación concreta de la lógica de negocio)      │
│  - execute(ExecutionContext) → TaskResult              │
└─────────────────────────────────────────────────────────┘
```

#### **1. Task (Nivel más bajo)**
Las **Tasks** son las implementaciones concretas donde reside la lógica de negocio. Son clases Java que extienden `FluxyTask`.

#### **2. Step (Nivel intermedio)**
Los **Steps** son unidades configurables que agrupan una o más tasks. La relación se gestiona mediante `StepTask`, que añade orden, estado y resultado a cada task en el contexto del step.

#### **3. Flow (Nivel más alto)**
Los **Flows** representan flujos de negocio completos. Conocen los steps mediante `FlowStep` (que añade orden y estado) y pueden bifurcarse mediante `Connection`s condicionales.

### Clases Intermediarias

Las clases intermediarias son clave para el desacoplamiento:

| Clase | Responsabilidad |
|-------|----------------|
| **`StepTask`** | Vincula una `FluxyTask` a un `FluxyStep` con orden, estado y resultado |
| **`FlowStep`** | Vincula un `FluxyStep` a un `FluxyFlow` con orden y estado de ejecución |
| **`Connection`** | Define bifurcaciones entre `FlowStep`s basadas en condiciones |

**Principio:** Flow, Step y Task **no se conocen directamente** — solo a través de las clases intermediarias. Esto permite máxima reutilización.

### Contexto de Ejecución

El **`ExecutionContext`** es el corazón del sistema:

- 📦 **Almacena variables** generadas durante la ejecución (accesibles por las tasks)
- 🔗 **Mantiene referencias** externas (IDs de entidades, recursos, etc.)
- 📊 **Registra la traza completa** en `ExecutionMetaInf` (qué tasks se ejecutaron, en qué orden, con qué resultado)
- 🔄 **Permite reutilización** — múltiples contextos pueden ejecutar el mismo flow independientemente

---

## 📦 Instalación

### Gradle

```gradle
dependencies {
    implementation 'org.fluxy:fluxy-core:0.0.2-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>org.fluxy</groupId>
    <artifactId>fluxy-core</artifactId>
    <version>0.0.2-SNAPSHOT</version>
</dependency>
```

---

## 📖 Guía de Uso

### 1. Crear una Task

Las tasks son clases que extienden `FluxyTask` e implementan la lógica de negocio:

```java
import org.fluxy.core.model.*;

public class ValidateOrderTask extends FluxyTask {
    
    public ValidateOrderTask() {
        this.name = "validate-order";
    }
    
    @Override
    public TaskResult execute(ExecutionContext context) {
        // Leer datos del contexto
        String orderId = context.getVariable("orderId").orElse(null);
        
        if (orderId == null || orderId.isEmpty()) {
            context.addParameter("validationError", "Order ID is required");
            return TaskResult.FAILURE;
        }
        
        // Simular validación
        boolean isValid = orderId.startsWith("ORD-");
        
        if (isValid) {
            context.addParameter("validated", "true");
            context.addReference("validatedOrderId", orderId);
            return TaskResult.SUCCESS;
        } else {
            context.addParameter("validationError", "Invalid order format");
            return TaskResult.FAILURE;
        }
    }
}
```

**Puntos clave:**
- ✅ Accede y modifica el `ExecutionContext`
- ✅ Retorna `TaskResult.SUCCESS` o `TaskResult.FAILURE`
- ✅ Puede lanzar excepciones (se capturan como `TaskExecutionException`)

### 2. Configurar Steps

Un step agrupa una o más tasks con orden y estado:

```java
import org.fluxy.core.model.*;
import java.util.List;

// Crear las tasks
FluxyTask validateTask = new ValidateOrderTask();
FluxyTask enrichTask = new EnrichOrderDataTask();
FluxyTask calculateTask = new CalculateTotalsTask();

// Crear el step con sus tasks (via StepTask)
FluxyStep validationStep = new FluxyStep();
validationStep.setName("order-validation");
validationStep.setTasks(List.of(
    new StepTask(1, validateTask),
    new StepTask(2, enrichTask),
    new StepTask(3, calculateTask)
));
```

**Las tasks se ejecutan en orden** cuando el step está activo.

### 3. Definir un Flow

Un flow orquesta steps y puede incluir bifurcaciones:

```java
import org.fluxy.core.model.*;
import java.util.List;

// Crear steps
FluxyStep validationStep = createValidationStep();
FluxyStep processingStep = createProcessingStep();
FluxyStep notificationStep = createNotificationStep();

// Envolver steps en FlowSteps (añade orden y estado)
FlowStep fs1 = new FlowStep(1, validationStep, StepStatus.PENDING);
FlowStep fs2 = new FlowStep(2, processingStep, StepStatus.PENDING);
FlowStep fs3 = new FlowStep(3, notificationStep, StepStatus.PENDING);

// Definir el flow
FluxyFlow orderFlow = new FluxyFlow();
orderFlow.setName("order-processing-flow");
orderFlow.setType("order");
orderFlow.setDescription("Flujo completo de procesamiento de órdenes");
orderFlow.setSteps(List.of(fs1, fs2, fs3));
orderFlow.setConnections(List.of()); // Sin bifurcaciones (flujo lineal)
```

### 4. Ejecutar el Flow

```java
import org.fluxy.core.service.*;
import org.fluxy.core.model.*;

// 1. Configurar servicios
FluxyEventsBus eventsBus = new InMemoryFluxyEventsBus(); // o tu implementación
TaskExecutorService taskExecutor = new TaskExecutorService(eventsBus);
StepExecutionService stepExecutor = new StepExecutionService(taskExecutor);
FlowExecutor flowExecutor = new FlowExecutor(eventsBus, stepExecutor);

// 2. Crear contexto de ejecución
ExecutionContext context = new ExecutionContext("order-processing", "1.0");
context.addParameter("orderId", "ORD-12345");
context.addParameter("customerId", "CUST-001");

// 3. Inicializar ejecución
flowExecutor.initializeExecution(orderFlow, context);

// 4. Ejecutar el flow paso a paso (o en loop hasta completar)
while (hasMoreSteps(context)) {
    flowExecutor.processFlow(orderFlow, context);
}

// 5. Verificar resultados
System.out.println("Validated: " + context.getVariable("validated").orElse("false"));
System.out.println("Processed: " + context.getVariable("processed").orElse("false"));
System.out.println("Notified: " + context.getVariable("notified").orElse("false"));

// 6. Inspeccionar traza de ejecución
ExecutionMetaInf metaInf = context.getExecutionMetaInf();
metaInf.getExecution().forEach((flowStep, stepTasks) -> {
    System.out.println("Step: " + flowStep.getStep().getName() 
                     + " - Status: " + flowStep.getStepStatus());
    stepTasks.forEach(st -> 
        System.out.println("  Task: " + st.getTask().getName() 
                         + " - Result: " + st.getResult()));
});
```

---

## 🔥 Ejemplos Avanzados

### Flujo con Bifurcación Condicional

```java
import org.fluxy.core.model.*;
import java.util.List;

// Steps
FluxyStep evaluationStep = createEvaluationStep(); // Determina prioridad
FluxyStep urgentProcessingStep = createUrgentProcessingStep();
FluxyStep normalProcessingStep = createNormalProcessingStep();
FluxyStep completionStep = createCompletionStep();

// FlowSteps
FlowStep fs1 = new FlowStep(1, evaluationStep, StepStatus.PENDING);
FlowStep fsUrgent = new FlowStep(2, urgentProcessingStep, StepStatus.PENDING);
FlowStep fsNormal = new FlowStep(3, normalProcessingStep, StepStatus.PENDING);
FlowStep fsFinal = new FlowStep(4, completionStep, StepStatus.PENDING);

// Condiciones
Condition highPriorityCondition = new Condition(
    StandardOperator.EQ,        // Operador
    "high",                      // Valor esperado
    "priority"                   // Variable del contexto
);

// Conexiones (bifurcaciones)
Connection toUrgent = new Connection(fs1, fsUrgent, List.of(highPriorityCondition));
Connection toCompletion = new Connection(fsUrgent, fsFinal, null); // Sin condición

// Flow con bifurcaciones
FluxyFlow priorityFlow = new FluxyFlow();
priorityFlow.setName("priority-routing-flow");
priorityFlow.setSteps(List.of(fs1, fsUrgent, fsNormal, fsFinal));
priorityFlow.setConnections(List.of(toUrgent, toCompletion));

// Ejecución
ExecutionContext context = new ExecutionContext("routing", "1.0");
flowExecutor.initializeExecution(priorityFlow, context);

// Primera ejecución: evaluationStep establece priority="high"
flowExecutor.processFlow(priorityFlow, context);

// Segunda ejecución: la condición se cumple → va a urgentProcessingStep
// (se salta normalProcessingStep)
flowExecutor.processFlow(priorityFlow, context);

// Tercera ejecución: va directo a completionStep
flowExecutor.processFlow(priorityFlow, context);
```

**¿Qué pasó?**
1. `evaluationStep` añade `priority="high"` al contexto
2. El motor evalúa las conexiones desde `fs1`
3. `toUrgent` cumple la condición → salta a `fsUrgent`
4. Desde `fsUrgent`, `toCompletion` (sin condición) lleva a `fsFinal`
5. **`fsNormal` nunca se ejecutó** ✅

### Reutilización de Steps

El mismo step puede usarse en múltiples flows:

```java
// Step compartido
FluxyTask validationTask = new CommonValidationTask();
FluxyStep sharedValidationStep = new FluxyStep();
sharedValidationStep.setName("shared-validation");
sharedValidationStep.setTasks(List.of(new StepTask(1, validationTask)));

// Flow A
FlowStep fsA1 = new FlowStep(1, sharedValidationStep, StepStatus.PENDING);
FlowStep fsA2 = new FlowStep(2, createFlowASpecificStep(), StepStatus.PENDING);
FluxyFlow flowA = new FluxyFlow();
flowA.setName("flow-A");
flowA.setSteps(List.of(fsA1, fsA2));

// Flow B (reutiliza el mismo step)
FlowStep fsB1 = new FlowStep(1, sharedValidationStep, StepStatus.PENDING);
FlowStep fsB2 = new FlowStep(2, createFlowBSpecificStep(), StepStatus.PENDING);
FluxyFlow flowB = new FluxyFlow();
flowB.setName("flow-B");
flowB.setSteps(List.of(fsB1, fsB2));

// Ejecutar con contextos independientes
ExecutionContext contextA = new ExecutionContext("flow-A", "1.0");
flowExecutor.initializeExecution(flowA, contextA);
// ... ejecutar flowA

ExecutionContext contextB = new ExecutionContext("flow-B", "1.0");
flowExecutor.initializeExecution(flowB, contextB);
// ... ejecutar flowB
```

**Ventajas:**
- ✅ DRY — no duplicar lógica de validación
- ✅ Los contextos son independientes
- ✅ Facilita testing y mantenimiento

---

## 📚 API Reference

### Core Interfaces

#### `FluxyTask` (abstracta)
```java
public abstract class FluxyTask {
    protected String name;
    public abstract TaskResult execute(ExecutionContext executionContext);
}
```

#### `Operator` (interfaz)
```java
public interface Operator {
    boolean matches(Object expectedValue, Object actualValue);
}
```

**Implementaciones disponibles:** `StandardOperator` (enum con EQ, NEQ, GT, LT, CONTAINS)

### Servicios

#### `FlowExecutor`
```java
// Inicializar contexto para un flow
void initializeExecution(FluxyFlow flow, ExecutionContext context);

// Ejecutar el siguiente paso del flow
void processFlow(FluxyFlow flow, ExecutionContext context);
```

#### `StepExecutionService`
```java
// Procesar un step (ejecuta la siguiente task disponible)
void processStep(FluxyStep step, ExecutionContext context);
```

#### `TaskExecutorService`
```java
// Ejecutar una task individual
TaskResult executeTask(FluxyTask task, ExecutionContext context);
```

### Modelo

#### `ExecutionContext`
```java
// Variables
void addParameter(String name, String value);
Optional<String> getVariable(String name);
Object getVariableByPath(String variablePath);

// Referencias
void addReference(String type, String value);
Optional<String> getReference(String type);

// Metadata
ExecutionMetaInf getExecutionMetaInf();
```

#### `Condition`
```java
boolean evaluate(ExecutionContext context);
```

### Enums

- **`TaskStatus`**: PENDING, RUNNING, FINISHED
- **`StepStatus`**: PENDING, RUNNING, FINISHED
- **`TaskResult`**: SUCCESS, FAILURE

---

## 🗺️ Roadmap

- [x] Motor de ejecución de flujos con bifurcaciones
- [x] Sistema de eventos
- [x] Operadores condicionales extensibles
- [x] Cobertura de tests completa (81 tests)
- [ ] **Persistencia JPA** (librería separada `fluxy-persistence`)
- [ ] **DSL/Builder fluido** para construcción de flows
- [ ] **Panel de monitoreo** en tiempo real
- [ ] **Soporte para paralelismo** (steps concurrentes)
- [ ] **Retry y circuit breaker** integrados
- [ ] **Versionado de flows**

---

## 🤝 Contribución

Las contribuciones son bienvenidas. Por favor:

1. Fork el repositorio
2. Crea una rama feature (`git checkout -b feature/amazing-feature`)
3. Commit tus cambios (`git commit -m 'Add amazing feature'`)
4. Push a la rama (`git push origin feature/amazing-feature`)
5. Abre un Pull Request

### Ejecutar tests

```bash
./gradlew test
```

---

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT. Ver el archivo [LICENSE](LICENSE) para más detalles.

---

## 📧 Contacto

Proyecto: [https://github.com/your-org/fluxy-core](https://github.com/your-org/fluxy-core)

---

**¿Preguntas?** Abre un [issue](https://github.com/your-org/fluxy-core/issues) o consulta la [wiki](https://github.com/your-org/fluxy-core/wiki).

