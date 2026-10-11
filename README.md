# TilkiInterface

TilkiCraft logosu ve turuncu/koyu kahve temasıyla Spigot **1.21.4**, Java **21** için oyuncu paneli ve ESC kaynak paketi.

## Kurulum

1. Sunucuyu durdur, eski `TilkiStatsHUD-*.jar` ve `TilkiInterface-*.jar` dosyalarını kaldır, `TilkiInterface-1.4.3.jar` dosyasını `plugins` klasörüne koy ve yeniden başlat. Eski `plugins/TilkiStatsHUD/config.yml` dosyası ilk açılışta `plugins/TilkiInterface/config.yml` yoluna kopyalanır. Resmî kaynak paketi bağlantısı otomatik güncellenir.
2. Oyuncu kaynak paketini kabul eder. ZIP GitHub'dan otomatik indirilir; sunucu klasörüne ZIP koymak gerekmez.
3. AuthMe varsa panel yalnızca başarılı girişten sonra açılır. AuthMe yoksa normal açılır.
4. `/tilkiui` veya `/istatistik`: paneli/üst şeridi aç-kapat. `/tilkiui reload`: yönetici ayar yenileme.

Paket: https://github.com/ardaacala/TilkiInterface/releases/tag/v1.4.3

## Gösterilen bilgiler

Çevrimiçi sayısı, kapasite, ping, beyaz oyuncu adı, rütbe rozeti, Vault bakiyesi, PlaceholderAPI üzerinden ada seviyesi ve Minecraft'ın kaydettiği toplam oyun süresi. Tarih Europe/Istanbul saat dilimini kullanır.

Vault ve ekonomi sağlayıcısı (ör. EssentialsX) varsa gerçek bakiye okunur. LuckPerms'in Vault Chat sağlayıcısı üzerinden prefix veya ana grup gösterilir. PlaceholderAPI `%luckperms_prefix%` çözerse o tercih edilir. Sağlayıcı yoksa bakiye `—`, rütbe ayarlardaki varsayılan olur. Uydurma bakiye/veri kullanılmaz.

Ada seviyesi varsayılan olarak `%superior_island_level_format%` ile okunur. `island-level.placeholder` değiştirilebilir; HUD satırında `{island-level}` kullanılır. PlaceholderAPI ve SuperiorSkyblock2 kurulu olmalıdır. Oy partisi veya etkinlik sayacı için bir veri kaynağı verilmediğinden bunlar eklenmedi. Satırlar başka PAPI placeholder'ları da kabul eder; en fazla 13 satır gösterilir.

## ESC ekranı

Kaynak paketi, ESC'nin `menu.game` başlığını logoyla ve mevcut düğmelerin dokusunu turuncu/kahve temayla değiştirir. Mevcut düğmelerin işlevi ve dizilimi Minecraft'a aittir. Yeni düğme, tıklanabilir /vote alanı veya referanstaki birebir özel yerleşim vanilla Spigot ile eklenmez; istemci modu gerekir. Logolu başlık Türkçe, İngilizce ABD ve İngilizce UK dillerinde tanımlıdır.

Düğme dokuları genel Minecraft widget'larıdır: seçenekler gibi başka ekranlarda da tema görünür. Logo kaynağı `CombatGuns/tilkicraft_logo.png`; tasarım değiştirilmeden 128x128'e ölçeklenir. Bitmap hücreleri 256x256 font atlası sınırını aşmaz. Unicode font desteği vardır.

Referansın yerleşimini temel alan `art/interface-preview.png` **tasarım önizlemesidir**, oyun ekran görüntüsü değildir. Sidebar'ın gerçek zemini Minecraft scoreboard zeminidir; önizlemedeki turuncu panel çizgisi scoreboard arka planını değiştiren bir shader değildir.

## Uyumluluk

- CombatGuns ve TilkiLoginScreen paketlerinin yanına farklı UUID ile eklenir; varsayılan istek gecikmesi 100 tick. Kendi font namespace'i ve EA00–EA01 karakterlerini kullanır. Silah crosshair/scope veya giriş title kanalına yazmaz; üst şerit bossbar kullanır.
- AuthMe giriş kontrolü başarısızsa panel gizli kalır; kimlik doğrulamayı bu plugin yönetmez.
- Sidebar için oyuncuya özel scoreboard gerekir. `sidebar.respect-existing: true` ile başka sidebar görünüyorsa zorla değiştirilmez. Diğer plugin sonradan scoreboard değiştirirse onun board'una dokunulmaz. Nametag/team veya başka scoreboard pluginleri kullanan sunucularda onların panelini kapatıp bu plugini tek sidebar sağlayıcısı yapmak gerekebilir; tüm üçüncü taraf scoreboard özelliklerini birleştirmez.
- Devre dışı bırakmada, oyuncu hâlâ bu pluginin scoreboard'unu kullanıyorsa önceki scoreboard geri yüklenir. Bossbar kaldırılır.
- Vanilla Spigot API scoreboard sayılarının biçimini sunmaz. Sunucu iç API'si destekliyorsa boş sayı biçimi uygulanır; desteklemiyorsa sağdaki sıra sayıları kalabilir. Bu durumda panelin çalışması kesilmez.
- Kaynak paketi reddedilirse metin paneli çalışır, ESC/logo teması görünmez. `pack.required` ile zorunlu yapılabilir. Paket ilk indirilmeden tema gösterilemez. Başka bir plugin tüm kaynak paketlerini sıfırlarsa temayı yeniden göndermek gerekir.
- Hedef istemci 1.21.4'tür; ViaVersion eski/yeni istemcilerin kaynak paketi uyumluluğunu tek başına sağlamaz. Çoklu sürüm paketleri bu sürüme dahil değildir.

## Geliştirme ve doğrulama

`mvn package` (Java 21). `java tools/BuildInterfacePack.java ../CombatGuns/tilkicraft_logo.png` paketi ve tasarım önizlemesini üretir.

Derleme, sayı/süre/placeholder testleri, font boyutları, dil JSON'ları, GUI nine-slice tanımları, ZIP kökü ve yayımlanan dosyanın SHA1'i doğrulanır. Canlı Minecraft istemcisi veya sunucuya erişim olmadığından ESC konumu, font davranışı ve tüm kurulu pluginlerle birlikte görünüm henüz denenmemiştir.

Canlı kontrol: kayıt/giriş öncesinde panel yok, giriş sonrasında panel var; bakiye ve rütbe gerçek; paket reddi durumunda metin başlık; Unicode açık/kapalı; farklı GUI ölçeklerinde ESC; CombatGuns scope ve giriş logosu; diğer scoreboard pluginiyle birlikte davranış; `/tilkiui` ve `/tilkiui reload`.

GUI sprite bilgisi: https://www.minecraft.net/nb-no/article/minecraft-java-edition-1-20-2

## 1.0.1 — TilkiTags rozet bağlantısı

TilkiTags 1.1.0 kuruluysa sidebar ve üst şerit `{rank}` alanında aynı tek parça görsel rozet gösterilir. `sidebar.use-tilkitags` varsayılan true'dur; eski config dosyasında alanın bulunması gerekmez. Oyuncu tag paketini henüz yüklemediyse metin fallback'i gösterilir. TilkiTags yoksa/eskise/devre dışıysa önceki Vault/PAPI davranışı devam eder. HUD kaynak paketi değişmedi; yalnızca JAR güncellemesi gerekir. Toplam 4 test geçti; canlı sunucuda görüntü henüz denenmedi.
## 1.2.0 — Kompakt HUD ve TAB çerçevesi

HUD metne göre 128–192 piksel genişlik seçer. Ölüm/öldürme satırları kaldırılır; kalan satırlarda özgün piksel simgeler görünür. Sidebar yalnız tilki maskotunu, TAB/ESC tam logoyu gösterir. Maskot `art/tilki-mascot.png`; imagegen yerleşik aracı ile uygulanan istem `art/mascot-prompt.txt` dosyasındadır.

TAB çerçevesi görünür oyuncu sayısına ve isim genişliğine göre 200 bitmap varyantından seçilir. Küçük pencere veya yüksek GUI ölçeğinde kırpılma olabilir; oyun içi doğrulama gereklidir. Tam güncelleme: https://github.com/ardaacala/TilkiResourcePack/releases/tag/v1.0.1

## 1.3.0 — HD arayüz

Atlas sınırına uygun 4x çerçeve parçaları, alt yazıda font ölçülerine göre ortalama ve Paper/Spigot boş skor sayı formatı. `art/ornate-frame.png` imagegen yerleşik aracıyla üretildi; kullanılan istem `art/ornate-frame-prompt.txt` dosyasındadır. Genel font seçimi TilkiResourcePack 1.0.2 üzerinden sunucu genelinde yapılır.

Bu bölüm eski 1.3.0 sürümünü açıklar. Güncel sürümde genel font seçimi ve Noto dosyaları kaldırılmıştır.

## 1.4.3 — Çerçevesiz logolar

HUD ve TAB çerçeveleri kaldırıldı. HUD üzerinde küçük [tilki başı](art/hud-fox-head.png) bulunur. TAB üzerinde yazılardan ayrı bir satırda [TilkiCraft logosu](art/tab-playerlist-logo.png) gösterilir. [Önizleme](art/logo-layout-preview.png) tasarım amaçlıdır; oyun ekran görüntüsü değildir. ESC logosu, rozetler ve diğer tema varlıkları korunur. Genel yazı fontu birleşik paketteki ASCII atlasından gelir.

Paket yeniden üretimi: Gson classpath'i ile `tools/BuildLogoAssets.java art/hud-fox-head.png art/tab-playerlist-logo.png` çalıştırılır. Araç eski çerçeve varlıklarını siler ve ZIP/SHA1 dosyalarını üretir.
