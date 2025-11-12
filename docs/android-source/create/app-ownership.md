# Configure and Handle Update Ownership for Apps

**Source:** https://source.android.com/docs/setup/create/app-ownership
**Scrape Date:** 2025-10-23
**Note:** Content was scraped in French.

---

**(Verbatim content in French)**

Lorsqu'une application est installée par une plate-forme de téléchargement ou un programme d'installation, la plate-forme de téléchargement ou le programme d'installation sont considérés comme l'installateur de référence, c'est-à-dire le dernier installateur de l'application. Avant Android 14, Android permettait à une autre plate-forme de téléchargement ou à un autre programme d'installation d'application de devenir l'installateur de référence et de mettre à jour l'application sans en informer l'utilisateur.

Dans Android 14, le programme d'installation initial d'une application peut se déclarer "propriétaire des mises à jour" et posséder les mises à jour de l'application. Si un autre programme d'installation tente de mettre à jour l'application, l'utilisateur a la possibilité d'approuver la nouvelle mise à jour avant qu'elle ne soit effectuée.


## ACTIVER LES PACKAGES POUR METTRE À JOUR LA PROPRIÉTÉ

Pour déclarer qu'un magasin ou un programme d'installation possède un package d'application, incluez la balise `update-ownership` dans votre fichier XML `sysconfig` pour chaque package, comme suit :

```xml
<update-ownership package="com.example.application" installer="com.example.installer" />
```

Dans cet exemple, `com.example.application` correspond au package d'application à posséder et `com.example.installer` au propriétaire du package. Lorsqu'un package est activé pour la propriété de la mise à jour, les autres plates-formes de téléchargement ou programmes d'installation privilégiés doivent gérer le propriétaire de la mise à jour et obtenir le consentement de l'utilisateur pour mettre à jour l'application.


## DÉSACTIVER LES MODIFICATIONS DE PROPRIÉTÉ POUR LES PACKAGES

Votre plate-forme de téléchargement ou votre programme d'installation peuvent désactiver un sous-ensemble de packages pour les modifications du propriétaire de la mise à jour en fournissant une liste de refus dans l'APK. Si vous incluez un package dans cette liste, aucun magasin ni programme d'installation ne pourra demander la propriété d'une mise à jour pour ce package.

Pour empêcher la mise à jour de packages par un autre magasin ou programme d'installation :

1.  Incluez la propriété suivante dans le fichier `AndroidManifest.xml` du magasin ou de l'installateur d'origine :
    
    ```xml
    <application …>
      <property android:name="android.app.PROPERTY_LEGACY_UPDATE_OWNERSHIP_DENYLIST"
                android:resource="@xml/legacyOwnershipDenylist" />
    </application>
    ```
    
    Cet exemple fait référence à une liste de refus XML appelée `legacyOwnershipDenylist`.

2.  Créez une liste de refus en tant que ressource XML brute au format suivant :
    
    ```xml
    <deny-ownership>com.example.app1</deny-ownership>
    <deny-ownership>com.example.app2</deny-ownership>
    ```

Si un magasin ou un programme d'installation demande la propriété d'un package figurant sur une liste de refus, la propriété ne sera pas accordée. Le package sera toujours installé, mais ne sera la propriété d'aucun programme d'installation. De plus, quel que soit le programme d'installation, une application figurant sur une liste de refus ne peut appartenir à personne.

L'ensemble des packages de cette liste peut changer lors d'une mise à jour de l'APK de l'installateur qui fournit la liste. Toute propriété définie pour un package qui est ensuite ajouté à une liste de refus est effacée lorsque le programme d'installation est mis à jour. Par conséquent, les mises à jour ultérieures du package d'application figurant sur la liste de refus ne nécessiteront pas d'interaction de l'utilisateur.


## GÉRER LE CHANGEMENT DE PROPRIÉTAIRE ET OBTENIR LE CONSENTEMENT DE L'UTILISATEUR

Avec Android 14, même si un installateur de plates-formes ou d'applications dispose de l'autorisation `android.permission.INSTALL_PACKAGES`, il doit toujours gérer l'état `STATUS_PENDING_USER_ACTION` s'il souhaite mettre à jour une application dont les mises à jour appartiennent à un autre installateur ou plate-forme.

L'application exemple `InstallAPKSessionApi.java` montre également comment gérer `STATUS_PENDING_USER_ACTION`.


## ÉTABLIR LA PROPRIÉTÉ DES APPLICATIONS PRÉCHARGÉES

Les applications préchargées n'appartiennent généralement pas à un installateur spécifique. Au lieu de cela, un nouveau propriétaire est attribué aux applications préchargées à l'aide de la configuration système, comme indiqué dans Activer les packages pour mettre à jour la propriété.
