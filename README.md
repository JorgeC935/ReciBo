# ♻️ ReciBo - Sistema de Recompensas Ecológicas  


## 📌 Tabla de Contenidos
- [🌱 Sobre el Proyecto](#-sobre-el-proyecto)
- [🎯 Propósito](#-propósito)
- [🛠 Tecnologías](#-tecnologías)
- [🏗 Estructura del Proyecto](#-estructura-del-proyecto)
- [⚙️ Instalación](#️-instalación)


## 🌱 Sobre el Proyecto
**Sistema de Devolución y Recompensa (SDR)** que transforma acciones ecológicas en beneficios:

Es un Sistema de Devolución y Recompensa (SDR) que funciona a través del móvil y premia el comportamiento medioambiental responsable. 
Los usuarios podrán escanear códigos QR en puntos de reciclaje, contenedores inteligentes o etiquetas de productos.
Por cada acción de reciclaje verificada (como llevar residuos a centros autorizados), recibirán puntos canjeables.


## 🎯 Propósito
**Revolucionar la gestión de residuos en Tarija mediante:**

El proyecto busca transformar la manera en que los ciudadanos de Tarija interactúan con sus residuos, convirtiendo el reciclaje en una actividad recompensada, accesible e informada, contribuyendo así a una ciudad más limpia y un futuro más sostenible.

## 🛠 Tecnologías
| Tecnología/Framework | Uso |  
|----------------------|-----|  
| [Kotlin](https://kotlinlang.org/) | Lenguaje principal para desarrollo Android |  
| [ZXing](https://github.com/zxing/zxing) | Escaneo de códigos QR/Bar |  
| [Firebase Auth](https://firebase.google.com/docs/auth) | Autenticación de usuarios (Email/Google/etc.) |  
| [Firebase Firestore](https://firebase.google.com/docs/firestore) *(opcional)* | Base de datos en la nube (si la usas) |  
| [Android Jetpack Components](https://developer.android.com/jetpack) | ViewModel, LiveData, Navigation, etc. |  
| [Glide](https://github.com/bumptech/glide) o [Picasso](https://square.github.io/picasso/) | Carga de imágenes (si aplica) |  
| [Retrofit](https://square.github.io/retrofit/) *(opcional)* | Consumo de APIs REST | 


```kotlin
dependencies {
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0' // QR Scanner
    implementation 'com.google.firebase:firebase-auth:22.2.0' // Autenticación
    implementation 'com.squareup.retrofit2:retrofit:2.9.0' // API Calls
}
```
```
qr-points-project/
├───backend/                  # API Flask
│   ├───app/
│   │   ├───models/           # Modelos de base de datos
│   │   │   ├───qr_code.py
│   │   │   ├───user.py
│   │   │   └───eco_partner.py
│   │   │
│   │   ├───services/         # Lógica de negocio
│   │   │   ├───qr_service.py
│   │   │   ├───auth_service.py
│   │   │   └───reward_service.py
│   │   │
│   │   ├───routes/           # Endpoints
│   │   │   ├───qr_routes.py
│   │   │   ├───user_routes.py
│   │   │   └───partner_routes.py
│   │   │
│   │   ├───utils/            # Utilidades
│   │   │   ├───database.py
│   │   │   └───crypto.py
│   │   │
│   │   └───config.py         # Configuraciones
│   │
│   ├───migrations/           # Migraciones de DB
│   ├───requirements.txt
│   └───run.py
│
├───mobile/                   # App Android
│   ├───app/
│   │   ├───data/
│   │   │   ├───api/          # Conexión API
│   │   │   │   ├───QRApiService.kt
│   │   │   │   └───models/   # DTOs
│   │   │   │
│   │   │   └───repository/   # Repositorios
│   │   │
│   │   ├───ui/
│   │   │   ├───qr/           # Pantallas QR
│   │   │   ├───rewards/      # Premios
│   │   │   └───profile/      # Perfil
│   │   │
│   │   └───utils/            # Utilidades
│   │
│   ├───build.gradle
│   └───app/src/main/res/      # Recursos
│
└───docs/                     # Documentación
    ├───db_schema.md          # Esquema de DB
    └───api_endpoints.md      # Endpoints

```

## ⚙️ Instalación  
```bash
git clone [tu-repo]  
cd [tu-app]  
npm install  
npm start
```
## Links:
Linkedin: https://www.linkedin.com/in/eri-colque-706512363/ 

Tiktok:https://www.tiktok.com/@recibo65?_t=ZM-8w20oUVmg8z&_r=1 

Facebook: [https://www.facebook.com/share/18zwreJTu4/ ](https://www.facebook.com/profile.php?id=61575795937149)


