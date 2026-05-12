# Plan de Implementación: Sistema de Notificaciones en la App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el historial de notificaciones y el indicador de "no leídas" (punto rojo) en la App de Android.

**Architecture:** Se creará un modelo de datos `Notification`, se actualizará el `ApiService` de Retrofit y se implementará un `NotificationAdapter` para el historial. El indicador visual se gestionará en el `BaseActivity` para que sea persistente en toda la app.

**Tech Stack:** Java, Android SDK, Retrofit, ViewBinding.

---

### Task 1: Modelo de Datos y ApiService

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/data/model/Notification.java`
- Modify: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

- [ ] **Step 1: Crear la clase Notification**
- [ ] **Step 2: Añadir endpoints de notificaciones a ApiService**

### Task 2: UI del Indicador (Punto Rojo)

**Files:**
- Modify: `app/src/main/res/layout/nav_drawer_header.xml`
- Create: `app/src/main/res/drawable/bg_notification_badge.xml`

- [ ] **Step 1: Crear el shape circular rojo**
- [ ] **Step 2: Envolver `ivNotifications` en un FrameLayout con el Badge en el XML**

### Task 3: Lógica del Badge en BaseActivity

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/BaseActivity.java`

- [ ] **Step 1: Crear método `updateNotificationBadge()` que llame a la API**
- [ ] **Step 2: Llamar a este método al abrir el Drawer**

### Task 4: Pantalla de Historial de Notificaciones

**Files:**
- Create: `app/src/main/res/layout/activity_notifications.xml`
- Create: `app/src/main/res/layout/item_notification.xml`
- Create: `app/src/main/java/com/codram/terecojo/NotificationsActivity.java`
- Create: `app/src/main/java/com/codram/terecojo/ui/adapter/NotificationAdapter.java`

- [ ] **Step 1: Crear el layout de la actividad y del item**
- [ ] **Step 2: Implementar el Adapter con lógica de colores por tipo**
- [ ] **Step 3: Implementar la actividad que cargue la lista desde la API**

### Task 5: Navegación y Marcado como Leído

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/BaseActivity.java`
- Modify: `app/src/main/java/com/codram/terecojo/NotificationsActivity.java`

- [ ] **Step 1: Configurar el clic en la campana para abrir `NotificationsActivity`**
- [ ] **Step 2: Lógica para marcar como leída al hacer clic en un item y navegar al recurso (solicitud_id)**
