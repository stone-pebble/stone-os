# Building Kernels

**Source:** https://source.android.com/docs/setup/build/building-kernels
**Scrape Date:** 2025-10-23
**Note:** Content was scraped in Turkish.

---

**(Verbatim content in Turkish)**

Bu sayfada, Android cihazlar için özel çekirdekler oluşturma süreci ayrıntılı olarak açıklanmaktadır. Bu talimatlar, doğru kaynakları seçme, çekirdeği oluşturma ve sonuçları Android Açık Kaynak Projesi'nden (AOSP) oluşturulan bir sistem görüntüsüne yerleştirme sürecinde size yol gösterir.

Not: Çekirdek kaynağı ödemesinin kökü tools/bazel içerir. Android ağacı yalnızca önceden oluşturulmuş çekirdek ikililerini içerir. Çekirdek ağaçları, çekirdek kaynaklarını ve Bazel dahil olmak üzere çekirdekleri oluşturmak için gereken tüm araçları içerir.


## KAYNAKLARI VE DERLEME ARAÇLARINI INDIRME

Son çekirdekler için kaynakları, araç zincirini ve derleme komut dosyalarını indirmek üzere repo komutunu kullanın. Bazı çekirdekler (ör. Pixel 3 çekirdekleri) birden fazla Git deposundan kaynak gerektirirken diğerleri (ör. ortak çekirdekler) yalnızca tek bir kaynak gerektirir. repo yaklaşımını kullanmak, kaynak dizinin doğru şekilde ayarlanmasını sağlar.

Uygun şubenin kaynaklarını indirin:

```bash
mkdir android-kernel && cd android-kernel
repo init -u https://android.googlesource.com/kernel/manifest -b BRANCH
repo sync
```

Önceki `repo init` komutuyla kullanılabilecek depo dallarının (BRANCH) listesi için Kernel branches and their build systems (Çekirdek dalları ve bunların derleme sistemleri) başlıklı makaleyi inceleyin.

Pixel cihazlar için çekirdekleri indirme ve derleme hakkında ayrıntılı bilgi edinmek için Pixel Çekirdeklerini Oluşturma başlıklı makaleyi inceleyin.

Not: Tek bir Repo ödemesi içinde farklı dallar arasında geçiş yapabilirsiniz. Yaygın çekirdek manifestleri (ve diğerlerinin çoğu), çekirdek git deposunun tamamen (sığ değil) klonlanacağını tanımlar. Bu, aralarında hızlı geçiş yapmayı sağlar. Farklı bir dala geçiş yapmak, bir dalı başlatmaya benzer. -u parametresi isteğe bağlıdır. Örneğin, mevcut depo ödemenizden common-android-mainline öğesine geçmek için:
`$ repo init -b common-android-mainline && repo sync` komutunu çalıştırın.


## ÇEKIRDEĞI OLUŞTURMA


### BAZEL ILE DERLEME (KLEAF)

Not: Kleaf'i ne zaman çekirdek oluşturmak, talimatlar ve komutlar oluşturmak için kullanabileceğinizi belirlemek için Çekirdek dalları ve derleme sistemleri başlıklı makaleyi inceleyin.

Android 13, Bazel ile çekirdek oluşturma özelliğini kullanıma sundu.

aarch64 mimarisi için GKI çekirdeği dağıtımı oluşturmak üzere Android 13'ten daha eski olmayan bir Android Ortak Çekirdeği dalını kontrol edin ve ardından aşağıdaki komutu çalıştırın:

```bash
tools/bazel run //common:kernel_aarch64_dist [-- --destdir=$DIST_DIR]
```

Ardından çekirdek ikili dosyası, modüller ve ilgili görüntüler `$DIST_DIR` dizininde yer alır. `--destdir` belirtilmemişse yapıtların konumunu öğrenmek için komutun çıkışına bakın. Ayrıntılar için AOSP ile ilgili dokümanlara bakın.


### BUILD.SH ILE DERLEME (ESKI)

Not: `build.sh`, Android 14 ve sonraki sürümlerde desteklenmez.

Android 12 veya önceki sürümlerdeki dallar YA DA Kleaf'siz dallar için:

```bash
build/build.sh
```

Not: Ortak çekirdekler genel, özelleştirilebilir çekirdeklerdir ve bu nedenle varsayılan bir yapılandırma tanımlamaz. Yaygın kullanılan çekirdekler için derleme yapılandırmasını nasıl belirleyeceğinizi öğrenmek için Çekirdek derlemesini özelleştirme başlıklı makaleye bakın. Örneğin, aarch64 platformu için GKI çekirdeğini oluşturmak üzere şu komutu çalıştırın:
`$ BUILD_CONFIG=common/build.config.gki.aarch64 build/build.sh`

Çekirdek ikili dosyası, modüller ve ilgili görüntü `out/BRANCH/dist` dizininde bulunur.


### SANAL CIHAZ IÇIN SATICI MODÜLLERINI OLUŞTURUN.

Android 13, `build.sh` yerine Bazel (Kleaf) ile çekirdek oluşturma özelliğini kullanıma sundu.

`virtual_device` modülleri için dağıtım oluşturmak üzere şunu çalıştırın:

```bash
tools/bazel run //common-modules/virtual-device:virtual_device_x86_64_dist [-- --destdir=$DIST_DIR]
```

Bazel ile Android çekirdekleri oluşturma hakkında daha fazla bilgi için şuraya bakın. Kleaf - Building Android Kernels with Bazel (Kleaf - Bazel ile Android Çekirdekleri Oluşturma).

Kleaf'in tek tek mimariler için desteğiyle ilgili ayrıntılar için Kleaf'in cihazlar ve çekirdekler için desteği başlıklı makaleyi inceleyin.


### BUILD.SH ILE SANAL CIHAZ IÇIN SATICI MODÜLLERINI OLUŞTURMA (ESKI)

Android 12'de Cuttlefish ve Goldfish birleştiğinden aynı çekirdeği paylaşır: `virtual_device`. Bu çekirdeğin modüllerini oluşturmak için şu derleme yapılandırmasını kullanın:

```bash
BUILD_CONFIG=common-modules/virtual-device/build.config.virtual_device.x86_64 build/build.sh
```

Android 11'de, çekirdeği Google tarafından bakımı yapılan bir çekirdek görüntüsü ve ayrı olarak oluşturulan, satıcı tarafından bakımı yapılan modüller şeklinde ayıran GKI tanıtıldı.

Bu örnekte bir çekirdek görüntüsü yapılandırması gösterilmektedir:

```bash
BUILD_CONFIG=common/build.config.gki.x86_64 build/build.sh
```

Bu örnekte bir modül yapılandırması (Cuttlefish ve Emulator) gösterilmektedir:

```bash
BUILD_CONFIG=common-modules/virtual-device/build.config.cuttlefish.x86_64 build/build.sh
```


## ÇEKIRDEĞI ÇALIŞTIRMA

Özel olarak oluşturulmuş bir çekirdeği çalıştırmanın birden fazla yolu vardır. Aşağıda, çeşitli geliştirme senaryolarına uygun bilinen yöntemler verilmiştir.


### ANDROID GÖRÜNTÜ DERLEMESINE YERLEŞTIRME

`Image.lz4-dtb` dosyasını AOSP ağacındaki ilgili çekirdek ikili konumuna kopyalayın ve önyükleme görüntüsünü yeniden oluşturun.

Alternatif olarak, `make bootimage` (veya önyükleme görüntüsü oluşturan başka bir make komut satırı) kullanırken `TARGET_PREBUILT_KERNEL` değişkenini tanımlayın. Bu değişken, `device/common/populate-new-device.sh` üzerinden ayarlandığı için tüm cihazlar tarafından desteklenir. Örneğin:

```bash
export TARGET_PREBUILT_KERNEL=DIST_DIR/Image.lz4-dtb
```


### FASTBOOT ILE ÇEKIRDEKLERI FLAŞLAMA VE BAŞLATMA

En yeni cihazlarda, önyükleme görüntüsü oluşturma ve önyükleme sürecini kolaylaştırmak için bir önyükleyici uzantısı bulunur.

Kernel'i yanıp sönme olmadan başlatmak için:

```bash
adb reboot bootloader
fastboot boot Image.lz4-dtb
```

Bu yöntem kullanıldığında çekirdek aslında yüklenmez ve yeniden başlatma işleminden sonra kalıcı olmaz.

Not: Çekirdek adları cihaza göre farklılık gösterir. Çekirdeğiniz için doğru dosya adını bulmak üzere AOSP ağacındaki `device/VENDOR/NAME-kernel` bölümüne bakın.


### CUTTLEFISH'TE ÇEKIRDEKLERI ÇALIŞTIRMA

Cuttlefish cihazlarında istediğiniz mimaride çekirdekler çalıştırabilirsiniz.

Bir Cuttlefish cihazını belirli bir çekirdek yapılandırması ile başlatmak için hedef çekirdek yapılandırmalarını parametre olarak kullanarak `cvd create` komutunu çalıştırın. Aşağıdaki örnek komutta, `common-android14-6.1` çekirdek manifestindeki bir `arm64` hedefi için çekirdek yapıları kullanılmaktadır.

```bash
cvd create \
    -kernel_path=/$PATH/$TO/common-android14-6.1/out/android14-6.1/dist/Image \
    -initramfs_path=/$PATH/$TO/common-android14-6.1/out/android14-6.1/dist/initramfs.img
```

Daha fazla bilgi için Cuttlefish'te çekirdek geliştirme başlıklı makaleyi inceleyin.


## ÇEKIRDEK DERLEMESINI ÖZELLEŞTIRME

Kleaf derlemeleri için çekirdek derlemelerini özelleştirme hakkında bilgi edinmek istiyorsanız Kleaf dokümanlarına bakın.

Not: Genel olarak, Kleaf derleme süreci ortam değişkenlerinden değil, komut satırı seçeneklerinden ve BUILD tanımlarından etkilenir.


### BUILD.SH ILE ÇEKIRDEK DERLEMESINI ÖZELLEŞTIRME (ESKI)

`build/build.sh` için derleme süreci ve sonucu ortam değişkenlerinden etkilenebilir. Çoğu isteğe bağlıdır ve her çekirdek dalı uygun bir varsayılan yapılandırmayla birlikte gelmelidir. En sık kullanılanlar burada listelenmiştir. Tam (ve güncel) liste için `build/build.sh` bölümüne bakın.

| Ortam değişkeni | Açıklama | Örnek |
| :--- | :--- | :--- |
| `BUILD_CONFIG` | Derleme ortamını başlattığınız yerden derleme yapılandırma dosyası. Konum, depo kök dizinine göre tanımlanmalıdır. Varsayılan olarak `build.config` değerine ayarlanır. Yaygın çekirdekler için zorunludur. | `BUILD_CONFIG=common/build.config.gki.aarch64` |
| `CC` | Kullanılacak derleyiciyi geçersiz kılın. `build.config` tarafından tanımlanan varsayılan derleyiciye geri döner. | `CC=clang` |
| `DIST_DIR` | Çekirdek dağıtımı için temel çıkış dizini. | `DIST_DIR=/path/to/my/dist` |
| `OUT_DIR` | Çekirdek derlemesi için temel çıkış dizini. | `OUT_DIR=/path/to/my/out` |
| `SKIP_DEFCONFIG` | Atla `make defconfig` | `SKIP_DEFCONFIG=1` |
| `SKIP_MRPROPER` | Atla `make mrproper` | `SKIP_MRPROPER=1` |


### YEREL DERLEMELER IÇIN ÖZEL ÇEKIRDEK YAPILANDIRMASI

Android 14 ve sonraki sürümlerde, çekirdek yapılandırmalarını özelleştirmek için defconfig parçalarını kullanabilirsiniz. Defconfig parçalarıyla ilgili Kleaf belgelerine bakın.

Not: Kleaf için LTO ayarlarını değiştirmek üzere `--lto` komut satırı seçeneğini kullanın.


### DERLEME YAPILANDIRMALARIYLA YEREL DERLEMELER IÇIN ÖZEL ÇEKIRDEK YAPILANDIRMASI (ESKI)

Android 13 ve önceki sürümlerde aşağıdakilere bakın.

Bir çekirdek yapılandırma seçeneğini düzenli olarak değiştirmeniz gerekiyorsa (ör. bir özellik üzerinde çalışırken) veya bir seçeneğin geliştirme amacıyla ayarlanması gerekiyorsa derleme yapılandırmasının yerel bir değişikliğini ya da kopyasını koruyarak bu esnekliği sağlayabilirsiniz.

Değişkeni `POST_DEFCONFIG_CMDS`, normal `make defconfig` adımı tamamlandıktan hemen sonra değerlendirilen bir ifadeye ayarlayın. `build.config` dosyaları derleme ortamına aktarıldığından, `build.config` içinde tanımlanan işlevler, post-defconfig komutlarının bir parçası olarak çağrılabilir.

Geliştirme sırasında çapraz tarama çekirdekleri için bağlantı zamanı optimizasyonunun (LTO) devre dışı bırakılması yaygın bir örnektir. LTO, yayınlanan çekirdekler için faydalı olsa da derleme zamanındaki ek yük önemli olabilir. Yerel `build.config` dosyasına eklenen aşağıdaki snippet, `build/build.sh` kullanılırken LTO'yu kalıcı olarak devre dışı bırakır.

```bash
POST_DEFCONFIG_CMDS="check_defconfig && update_debug_config"
function update_debug_config() {
    ${KERNEL_DIR}/scripts/config --file ${OUT_DIR}/.config \
         -d LTO \
         -d LTO_CLANG \
         -d CFI \
         -d CFI_PERMISSIVE \
         -d CFI_CLANG
    (cd ${OUT_DIR} && \
     make O=${OUT_DIR} $archsubarch CC=${CC} CROSS_COMPILE=${CROSS_COMPILE} olddefconfig)
}
```


## ÇEKIRDEK SÜRÜMLERINI BELIRLEME

Derleme için doğru sürümü iki kaynaktan belirleyebilirsiniz: AOSP ağacı ve sistem görüntüsü.


### AOSP AĞACINDAKI ÇEKIRDEK SÜRÜMÜ

AOSP ağacı, önceden oluşturulmuş çekirdek sürümlerini içerir. Git günlüğü, doğru sürümü commit mesajının bir parçası olarak gösterir:

```bash
cd $AOSP/device/VENDOR/NAME
git log --max-count=1
```

Çekirdek sürümü git günlüğünde listelenmiyorsa aşağıda açıklandığı gibi sistem görüntüsünden alın.


### SISTEM GÖRÜNTÜSÜNDEKİ ÇEKIRDEK SÜRÜMÜ

Bir sistem görüntüsünde kullanılan çekirdek sürümünü belirlemek için çekirdek dosyasına karşı aşağıdaki komutu çalıştırın:

```bash
file kernel
```

`Image.lz4-dtb` dosyaları için şu komutu çalıştırın:

```bash
grep -a 'Linux version' Image.lz4-dtb
```


## BAŞLATMA GÖRÜNTÜSÜ OLUŞTURMA

Çekirdek derleme ortamını kullanarak bir önyükleme görüntüsü oluşturabilirsiniz.


### INIT_BOOT ÖZELLIKLI CIHAZLAR IÇIN ÖNYÜKLEME GÖRÜNTÜSÜ OLUŞTURMA

`init_boot` bölümü olan cihazlarda, başlatma görüntüsü çekirdekle birlikte oluşturulur. `initramfs` resmi, önyükleme görüntüsüne yerleştirilmemiştir.

Örneğin, Kleaf ile GKI önyükleme görüntüsünü aşağıdaki komutla oluşturabilirsiniz:

```bash
tools/bazel run //common:kernel_aarch64_dist [-- --destdir=$DIST_DIR]
```

`build/build.sh` (eski) ile GKI önyükleme görüntüsünü şu şekilde oluşturabilirsiniz:

```bash
BUILD_CONFIG=common/build.config.gki.aarch64 build/build.sh
```

GKI önyükleme görüntüsü `$DIST_DIR` konumunda bulunur.


### INIT_BOOT OLMAYAN CIHAZLAR IÇIN BAŞLATMA GÖRÜNTÜSÜ OLUŞTURMA (ESKI)

`init_boot` bölümü olmayan cihazlar için GKI önyükleme görüntüsü indirip açarak elde edebileceğiniz bir ramdisk ikilisine ihtiyacınız vardır. İlişkili Android sürümündeki tüm GKI önyükleme görüntüleri çalışır.

```bash
tools/mkbootimg/unpack_bootimg.py --boot_img=boot-5.4-gz.img
mv $KERNEL_ROOT/out/ramdisk gki-ramdisk.lz4
```

Hedef klasör, çekirdek ağacının en üst düzey dizinidir (mevcut çalışma dizini).

En son AOSP sürüm dalıyla geliştirme yapıyorsanız bunun yerine `ci.android.com` adresindeki bir `aosp_arm64` derlemesinden `ramdisk-recovery.img` derleme yapısını indirebilir ve bunu ramdisk ikiliniz olarak kullanabilirsiniz.

Bir ramdisk ikiliniz varsa ve bunu çekirdek derlemesinin kök dizinindeki `gki-ramdisk.lz4` konumuna kopyaladıysanız şu komutu çalıştırarak bir önyükleme görüntüsü oluşturabilirsiniz:

```bash
BUILD_BOOT_IMG=1 SKIP_VENDOR_BOOT=1 KERNEL_BINARY=Image GKI_RAMDISK_PREBUILT_BINARY=gki-ramdisk.lz4 BUILD_CONFIG=common/build.config.gki.aarch64 build/build.sh
```

x86 tabanlı mimariyle çalışıyorsanız `Image` ifadesini `bzImage`, `aarch64` ifadesini ise `x86_64` ile değiştirin:

```bash
BUILD_BOOT_IMG=1 SKIP_VENDOR_BOOT=1 KERNEL_BINARY=bzImage GKI_RAMDISK_PREBUILT_BINARY=gki-ramdisk.lz4 BUILD_CONFIG=common/build.config.gki.x86_64 build/build.sh
```

Bu dosya, yapı dizininde `$KERNEL_ROOT/out/$KERNEL_VERSION/dist` yer alır.

Başlatma görüntüsü `out/<kernel branch>/dist/boot.img` konumunda bulunur.

```