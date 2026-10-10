# TilkiInterface

TilkiCraft logosu ve turuncu/koyu kahve temasıyla Spigot **1.21.4**, Java **21** için oyuncu paneli ve ESC kaynak paketi.

## Kurulum

1. Sunucuyu durdur, `TilkiInterface-1.0.0.jar` dosyasını `plugins` klasörüne koy ve yeniden başlat.
2. Oyuncu kaynak paketini kabul eder. ZIP GitHub'dan otomatik indirilir; sunucu klasörüne ZIP koymak gerekmez.
3. AuthMe varsa panel yalnızca başarılı girişten sonra açılır. AuthMe yoksa normal açılır.
4. `/tilkiui` veya `/istatistik`: paneli/üst şeridi aç-kapat. `/tilkiui reload`: yönetici ayar yenileme.

Paket: https://github.com/ardaacala/TilkiInterface/releases/tag/v1.0.0

## Gösterilen bilgiler

Çevrimiçi sayısı, kapasite, ping, ad, rütbe, Vault bakiyesi, Minecraft XP seviyesi, oyuncu öldürme/ölüm sayısı ve Minecraft'ın kaydettiği toplam oyun süresi. Tarih Europe/Istanbul saat dilimini kullanır.

Vault ve ekonomi sağlayıcısı (ör. EssentialsX) varsa gerçek bakiye okunur. LuckPerms'in Vault Chat sağlayıcısı üzerinden prefix veya ana grup gösterilir. PlaceholderAPI `%luckperms_prefix%` çözerse o tercih edilir. Sağlayıcı yoksa bakiye `—`, rütbe ayarlardaki varsayılan olur. Uydurma bakiye/veri kullanılmaz.

Ada seviyesi için `sidebar.island-placeholder` alanına kurulu SuperiorSkyblock/PAPI expansion'ının desteklediği placeholder'ı yazıp satırlardan birine `{island}` ekle. XP seviyesi ada seviyesi değildir. Oy partisi veya etkinlik sayacı için bir veri kaynağı verilmediğinden bunlar eklenmedi. Satırlar başka PAPI placeholder'ları da kabul eder; en fazla 15 satır gösterilir.

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
