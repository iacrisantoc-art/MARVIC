# 📚 Guía de Git para MARVIC

## 🔽 **RECIBIR CAMBIOS** (Git Pull)

### **Opción 1: Recibir cambios simples**
```bash
git pull origin master
```

### **Opción 2: Recibir cambios con rebase (recomendado)**
```bash
git pull --rebase origin master
```

### **Opción 3: Ver cambios antes de recibir**
```bash
# Ver qué cambios hay en GitHub
git fetch origin

# Ver diferencias
git diff master origin/master

# Si todo está bien, recibir los cambios
git pull origin master
```

---

## 📤 **SUBIR CAMBIOS** (Git Push)

### **1. Ver qué archivos cambiaron**
```bash
git status
```

### **2. Agregar archivos al staging**
```bash
# Agregar todos los archivos modificados
git add .

# O agregar archivos específicos
git add app/src/main/java/com/proyecto/marvic/MainActivity.kt
```

### **3. Hacer commit**
```bash
git commit -m "Descripción de los cambios realizados"
```

### **4. Subir a GitHub**
```bash
# Primera vez (configurar remote)
git remote add origin https://github.com/crack2116/MARVIC.git

# Subir cambios
git push -u origin master

# En siguientes veces, solo:
git push
```

---

## 🔄 **FLUJO COMPLETO DE TRABAJO**

### **Día a día:**
```bash
# 1. Recibir cambios del equipo
git pull origin master

# 2. Trabajar en tu código...

# 3. Ver qué cambió
git status

# 4. Agregar cambios
git add .

# 5. Hacer commit
git commit -m "Fix: Corregir bug en login"

# 6. Subir cambios
git push
```

---

## 🛠️ **COMANDOS ÚTILES**

### **Ver historial de commits**
```bash
git log --oneline
```

### **Ver diferencias antes de commit**
```bash
git diff
```

### **Deshacer cambios no guardados**
```bash
# Descartar cambios en un archivo
git checkout -- nombre_archivo.kt

# Descartar todos los cambios
git reset --hard HEAD
```

### **Deshacer último commit (mantener cambios)**
```bash
git reset --soft HEAD~1
```

### **Ver ramas**
```bash
git branch
```

### **Crear nueva rama**
```bash
git checkout -b nombre-rama
```

---

## ⚠️ **SOLUCIÓN DE PROBLEMAS**

### **Si hay conflictos al hacer pull:**
```bash
# 1. Git te dirá qué archivos tienen conflictos
# 2. Abre el archivo y busca las marcas:
#    <<<<<<< HEAD
#    (tu código)
#    =======
#    (código del remoto)
#    >>>>>>> origin/master

# 3. Resuelve los conflictos manualmente
# 4. Agrega los archivos resueltos
git add .

# 5. Completa el merge
git commit -m "Resolve merge conflicts"
```

### **Si necesitas forzar push (cuidado)**
```bash
# Solo si es absolutamente necesario
git push --force origin master
```

### **Ver configuración del remote**
```bash
git remote -v
```

### **Cambiar URL del remote**
```bash
git remote set-url origin https://github.com/crack2116/MARVIC.git
```

---

## 📋 **COMANDOS RÁPIDOS PARA ESTE PROYECTO**

### **Configurar por primera vez:**
```bash
cd "C:\Users\elcra\Desktop\PRACTICAS\PRACTCAS 2"
git remote add origin https://github.com/crack2116/MARVIC.git
git branch -M master
git push -u origin master
```

### **Recibir cambios:**
```bash
git pull origin master
```

### **Subir cambios:**
```bash
git add .
git commit -m "Tu mensaje de commit"
git push
```

---

## 💡 **MEJORES PRÁCTICAS**

1. **Siempre hacer pull antes de trabajar:**
   ```bash
   git pull origin master
   ```

2. **Commits descriptivos:**
   - ❌ Mal: `git commit -m "fix"`
   - ✅ Bien: `git commit -m "Fix: Corregir validación de email en LoginScreen"`

3. **Hacer commits frecuentes:**
   - No esperes días para hacer commit
   - Commits pequeños son más fáciles de revisar

4. **Verificar antes de push:**
   ```bash
   git status
   git log -1  # Ver último commit
   ```

---

## 🎯 **COMANDOS PARA ESTE MOMENTO**

Si ya hiciste cambios y quieres subirlos:

```bash
# 1. Ver qué cambió
git status

# 2. Agregar todo
git add .

# 3. Commit
git commit -m "Actualización: Mejoras en Firebase y documentación"

# 4. Subir
git push origin master
```

Si quieres recibir cambios desde GitHub:

```bash
# 1. Recibir cambios
git pull origin master

# Si hay conflictos, resuélvelos y luego:
git add .
git commit -m "Resolve conflicts"
```

