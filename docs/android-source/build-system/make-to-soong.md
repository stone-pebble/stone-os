# Convert from Make to Soong

**Source:** https://source.android.com/docs/setup/build/make-to-soong
**Scrape Date:** 2025-10-23
**Note:** Content was scraped in German.

---

**(Verbatim content in German)**

Vor der Veröffentlichung von Android 7.0 wurden die Build-Regeln in Android ausschließlich mit GNU Make beschrieben und ausgeführt. Das Make-Build-System wird weitgehend unterstützt und verwendet, aber bei der Größe von Android wurde es langsam, fehleranfällig, nicht skalierbar und schwer zu testen. Das Soong-Build-System bietet die für Android-Builds erforderliche Flexibilität. Wir nehmen wesentliche Änderungen am Android-Build-System vor. Das Make-Build-System (Android.mk) wird eingestellt und durch Soong (Android.bp) ersetzt.

Aus diesem Grund wird von Plattformentwicklern erwartet, dass sie so schnell wie möglich von Make zu Soong wechseln. Wenn Sie Support benötigen, können Sie Fragen an die Google-Gruppe Android Building senden.


## WAS IST SOONG?

Das Soong-Build-System wurde in Android 7.0 (Nougat) eingeführt, um Make zu ersetzen. Es nutzt das Kati-Tool, einen GNU Make-Klon, und die Ninja-Buildsystemkomponente, um Builds von Android zu beschleunigen.

Eine allgemeine Anleitung und Informationen zu den Änderungen am Build-System für Android.mk-Autoren finden Sie in der Beschreibung des Android Make Build System im Android Open Source Project (AOSP). Dort erfahren Sie auch, welche Änderungen erforderlich sind, um von Make zu Soong zu wechseln. Definitionen wichtiger Begriffe finden Sie im Glossar unter build-related entries. Ausführliche Informationen finden Sie in der Soong Modules Reference.

Achtung :Bis Android vollständig von Make zu Soong migriert ist, muss in der Make-Produktkonfiguration der Wert PRODUCT_SOONG_NAMESPACES angegeben werden. Eine Anleitung finden Sie unter Namespace-Module.


## VERGLEICH ZWISCHEN MAKE UND SOONG

Hier ist ein Vergleich der Make-Konfiguration mit Soong, bei dem dasselbe in einer Soong-Konfigurationsdatei (Blueprint oder .bp) erreicht wird.


### BEISPIEL ERSTELLEN

```make
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libxmlrpc++
LOCAL_MODULE_HOST_OS := linux

LOCAL_RTTI_FLAG := -frtti
LOCAL_CPPFLAGS := -Wall -Werror -fexceptions
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src

LOCAL_SRC_FILES := $(call \
     all-cpp-files-under,src)
include $(BUILD_SHARED_LIBRARY)
```

### SOONG-BEISPIEL

```json
cc_library_shared {
     name: "libxmlrpc++",

     rtti: true,
     cppflags: [
           "-Wall",
           "-Werror",
           "-fexceptions",
     ],
     export_include_dirs: ["src"],
     srcs: ["src/**/*.cpp"],

     target: {
           darwin: {
                enabled: false,
           },
     },
}
```

Beispiele für testspezifische Soong-Konfigurationen finden Sie unter Einfache Build-Konfiguration.


## GRUNDLEGENDES KONVERTIERUNGSVERFAHREN

Die Konvertierung einer `Android.mk`-Datei in eine `Android.bp`-Datei folgt in der Regel diesem allgemeinen Workflow mit dem `androidmk`-Hilfsprogramm. Bei der Konvertierung werden in der Regel die folgenden Schritte ausgeführt.

1.  Richten Sie die Terminalumgebung ein und erstellen Sie das Tool `androidmk`.
    
    `androidmk` ist ein Befehlszeilentool, das eine `Android.mk`-Datei parst und versucht, eine analoge `Android.bp`-Datei auszugeben. Die meisten `Android.mk`-Dateien können mit wenigen oder keinen manuellen Änderungen in `Android.bp` konvertiert werden.
    
    ```bash
    cd <root-of-the-tree>
    source build/envsetup.sh
    lunch <lunch-target>
    m androidmk
    ```

2.  Mit dem `Android.mk` erstellen: `sh m <module-name>`

3.  Führen Sie das `androidmk`-Konvertierungstool aus: `sh androidmk <path-to-Android.mk>/Android.mk > <path-to-Android.bp>/Android.bp`

4.  Bearbeiten Sie die Datei `Android.bp` manuell:
    
    *   Beheben Sie alle Warnungen, die vom `androidmk`-Tool ausgegeben werden.
    *   Behalten Sie eine Kopfzeile mit Urheberrechtsangaben bei oder fügen Sie eine hinzu. Wenn Sie eine neue hinzufügen, verwenden Sie das aktuelle Jahr.

5.  Entfernen Sie die Datei `Android.mk` und erstellen Sie den Build mit der Datei `Android.bp`.

6.  Validieren Sie die Konvertierung, indem Sie die erstellten Artefakte vergleichen oder Unit- und Funktionstests ausführen.

7.  Speichern Sie die Änderungen und laden Sie sie zur Überprüfung hoch.

Weitere Informationen finden Sie unter Android.bp-Dateiformat.

```