# Penjelasan Penambahan ORM dan Hibernate

Penambahan ORM dan Hibernate dilakukan pada lapisan akses data aplikasi. Sebelumnya, DAO menggunakan JDBC dan menulis query SQL secara manual, kemudian membaca hasilnya melalui `ResultSet`. Penambahan ini mengubah pengelolaan data dari pendekatan tabel dan kolom menjadi pendekatan objek dan field. ORM berfungsi sebagai penghubung antara kelas Java dan tabel database, sedangkan Hibernate merupakan library yang menjalankan mekanisme ORM tersebut.

Dalam implementasi ini, pembagian tugas dilakukan secara jelas. `Database` tetap menyiapkan tabel dan kolom. Kelas model diberi anotasi agar dikenali ORM. Hibernate dikonfigurasi untuk membuka koneksi, menyediakan session, membuat query, dan mengatur transaksi. DAO menggunakan session tersebut untuk membaca atau menyimpan data, sedangkan controller tetap memanggil DAO dan menampilkan pesan error apabila terjadi kegagalan.

## Penambahan ORM dan Buktinya

Penambahan ORM dilakukan dengan memberikan anotasi pada kelas model. Kelas `Kategori`, `Barang`, `Transaksi`, `Distributor`, `Pengadaan`, dan `Cashflow` telah diberi anotasi yang menghubungkannya dengan tabel database. Bukti penerapan ORM terlihat pada kelas `Kategori`:

```java
@Entity
@Table(name = "kategori")
public class Kategori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false, unique = true)
    public String nama;
```

Anotasi `@Entity` digunakan untuk menandai bahwa kelas tersebut merupakan entity yang dapat disimpan oleh Hibernate. Anotasi `@Table` menentukan nama tabel, yaitu `kategori`. Anotasi `@Id` menandai primary key, sedangkan `@GeneratedValue` dengan tipe `IDENTITY` menunjukkan bahwa nilai `id` dibuat otomatis oleh database. Anotasi `@Column` digunakan untuk menentukan aturan kolom, seperti tidak boleh kosong dan harus unik.

Pemetaan tersebut memungkinkan data dari tabel dibaca langsung sebagai objek `Kategori` tanpa pengambilan kolom satu per satu dari `ResultSet`. Sebaliknya, objek `Kategori` dapat disimpan ke tabel yang sesuai tanpa penulisan query insert secara manual. Proses penerjemahan antara objek Java dan baris database tersebut merupakan tugas utama ORM.

Pemetaan juga dilakukan pada field yang memiliki nama kolom berbeda. Pada kelas `Barang`, field `kategoriId` dipetakan ke kolom `kategori_id`, sedangkan field `keterangan` tetap menggunakan nama kolom yang sama. Field yang hanya digunakan untuk kebutuhan tampilan, seperti nama kategori, diberi anotasi `@Transient` agar tidak dianggap sebagai kolom database. Dengan demikian, ORM hanya menyimpan data yang tersedia pada tabel, tetapi aplikasi tetap dapat menggunakan data tambahan untuk keperluan tampilan.

## Penambahan Hibernate dan Buktinya

Penambahan Hibernate dilakukan dengan memasukkan JAR Hibernate ke dalam proyek. Proyek menggunakan `hibernate-core` dan dialect SQLite, tidak lagi hanya menggunakan driver SQLite:

```properties
file.reference.hibernate-core-6.6.59.Final.jar=${basedir}/lib/hibernate-core-6.6.59.Final.jar
file.reference.hibernate-community-dialects-6.6.59.Final.jar=${basedir}/lib/hibernate-community-dialects-6.6.59.Final.jar
```

JAR `hibernate-core` berisi mesin Hibernate yang menjalankan ORM. JAR `hibernate-community-dialects` ditambahkan karena menyediakan dialect SQLite. Dialect tersebut digunakan untuk menyesuaikan penulisan SQL dengan karakteristik SQLite, termasuk perbedaan sintaks dan tipe data.

Konfigurasi Hibernate ditempatkan pada kelas `HibernateUtil`. Pada kelas tersebut ditentukan driver, alamat database, dan dialect yang digunakan:

```java
cfg.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
cfg.setProperty("hibernate.connection.url", Database.URL);
cfg.setProperty("hibernate.dialect",
    "org.hibernate.community.dialect.SQLiteDialect");
cfg.setProperty("hibernate.hbm2ddl.auto", "none");
```

Properti `hibernate.hbm2ddl.auto=none` digunakan agar Hibernate tidak membuat atau mengubah tabel. Struktur database tetap dikelola oleh `Database.initSchema()`, sedangkan Hibernate digunakan untuk membaca dan menulis data. Seluruh kelas model yang telah diberi anotasi didaftarkan pada konfigurasi Hibernate. Setelah konfigurasi selesai, Hibernate membuat `SessionFactory` yang digunakan untuk membuka session pada setiap operasi DAO.

Tugas Hibernate dalam implementasi ini meliputi pembacaan anotasi ORM, pembukaan koneksi ke SQLite, penyediaan `Session`, penerjemahan operasi Java menjadi SQL, serta pengaturan transaksi. Kesalahan yang terjadi pada database diubah menjadi `HibernateException` agar dapat ditangani oleh controller. Dengan pembagian tersebut, DAO tidak perlu menulis SQL secara manual dan controller tidak perlu mengetahui proses internal Hibernate.

## Perubahan DAO

Setelah ORM dan Hibernate terpasang, DAO diubah agar menggunakan `Session` dari Hibernate. Sebelumnya, DAO berisi query SQL manual untuk mengambil dan menyimpan data. Pada implementasi baru, DAO meminta Hibernate mengambil atau menyimpan entity. Bukti perubahan tersebut terlihat pada `KategoriDAO`:

```java
public List<Kategori> findAll() {
    try (Session session = HibernateUtil.open()) {
        return session.createQuery(
            "from Kategori order by nama", Kategori.class
        ).getResultList();
    }
}
```

Pada kode tersebut tidak terdapat query `SELECT * FROM kategori`. Query Hibernate mengacu pada entity `Kategori`, kemudian Hibernate menerjemahkannya menjadi SQL yang sesuai dengan database. Session dibuka menggunakan `try-with-resources` sehingga otomatis ditutup setelah data selesai diambil.

Transaksi juga ditambahkan pada operasi penyimpanan. Contohnya pada proses penambahan kategori:

```java
Transaction tx = session.beginTransaction();

try {
    session.persist(new Kategori(nama));
    tx.commit();
} catch (RuntimeException e) {
    tx.rollback();
    throw e;
}
```

`session.persist` digunakan untuk menyimpan objek baru. `commit` digunakan untuk menyimpan perubahan secara permanen, sedangkan `rollback` digunakan untuk membatalkan perubahan apabila terjadi kesalahan. Pola tersebut diterapkan pada DAO lain agar data tidak tersimpan setengah jadi. Dengan cara ini, alur DAO tetap jelas tanpa penulisan perintah SQL satu per satu.

## Pengelolaan Stok dan Cashflow

Aturan stok yang telah ada tetap dipertahankan. `BarangDAO` tetap menolak perubahan yang menyebabkan stok menjadi negatif:

```java
if (item.stok + delta < 0) {
    throw new HibernateException("Stok barang tidak mencukupi.");
}

item.stok += delta;
```

Perubahan nilai `stok` tidak lagi memerlukan penulisan `UPDATE` secara manual. Field `stok` diubah melalui entity yang sedang dikelola session, kemudian Hibernate menyimpan perubahan tersebut saat transaksi selesai. Validasi stok tetap dilakukan sebelum perubahan sehingga aturan bisnis dan pesan kesalahan tidak berubah.

Pada proses penjualan, penyimpanan transaksi, pengurangan stok, dan pencatatan cashflow dilakukan dalam satu transaksi Hibernate:

```java
session.persist(transaksi);
session.flush();

CashflowDAO.insertSale(
    session, transaksi.id, date.toString(),
    namaBarang, price * qty
);
```

`session.flush()` digunakan agar ID transaksi yang baru dibuat langsung tersedia. ID tersebut kemudian digunakan untuk menghubungkan cashflow dengan transaksi penjualan. Karena seluruh proses berada dalam satu transaksi, kegagalan pada salah satu bagian akan membatalkan seluruh perubahan. Stok tidak berkurang apabila pencatatan penjualan gagal, dan cashflow tidak bertambah apabila penyimpanan transaksi gagal.

Pola yang sama diterapkan pada pengadaan dan penghapusan. Pada proses edit pengadaan, stok lama dikembalikan terlebih dahulu, kemudian stok baru ditambahkan. Pada penghapusan cashflow, transaksi atau pengadaan yang terhubung juga dihapus dan stok dikembalikan sesuai aturan yang berlaku. Seluruh proses tersebut dijalankan dalam transaksi agar data stok dan cashflow tetap konsisten.

## Penyesuaian Controller dan Main

Controller disesuaikan karena kesalahan dari Hibernate berbeda dengan kesalahan JDBC. Controller menangkap `HibernateException`, bukan `SQLException`. Contohnya pada pengelolaan pengadaan:

```java
} catch (HibernateException e) {
    view.showError(e.getMessage());
}
```

Penyesuaian tersebut dilakukan karena Hibernate melempar runtime exception, bukan checked exception seperti `SQLException`. Alur controller tidak diubah: controller tetap memanggil DAO, kemudian menampilkan pesan kesalahan ke view apabila operasi gagal. Perubahan hanya dilakukan pada jenis exception yang ditangkap.

Pada `Main`, inisialisasi Hibernate ditambahkan setelah pembuatan skema:

```java
Database.initSchema();
HibernateUtil.init();
```

`Database.initSchema()` tetap dijalankan karena Hibernate tidak mengelola pembuatan tabel. Setelah skema siap, `HibernateUtil.init()` membuat `SessionFactory`. Shutdown hook juga ditambahkan agar Hibernate ditutup saat aplikasi selesai:

```java
Runtime.getRuntime().addShutdownHook(
    new Thread(HibernateUtil::shutdown));
```

## Kompatibilitas Database

Kompatibilitas database lama dipertahankan dengan tidak menyerahkan pembuatan tabel kepada Hibernate. Karena `hibernate.hbm2ddl.auto=none`, Hibernate tidak membuat atau mengubah tabel. Pembuatan tabel, penambahan kolom, dan pengisian data lama tetap dilakukan oleh `Database.initSchema()`. Nama tabel dan kolom pada entity disamakan dengan database lama, termasuk `barang_id`, `nama_barang`, `distributor_id`, `transaksi_id`, dan `pengadaan_id`. Dengan demikian, database yang telah ada dapat dibuka tanpa migrasi dan data lama tidak perlu dibuat ulang.

Hubungan antar tabel juga tetap menggunakan ID biasa, misalnya `barangId` pada `Transaksi` dan `Pengadaan`. Nama barang dan nama distributor diisi oleh DAO saat data ditampilkan. Relasi objek yang kompleks tidak digunakan agar bentuk data yang telah dipakai view dan controller tidak berubah. Pendekatan ini menjaga agar penambahan ORM dan Hibernate tidak menyebabkan penulisan ulang seluruh aplikasi.

Dengan demikian, ORM berperan dalam pemetaan objek ke tabel, Hibernate berperan dalam pengelolaan session, query, dan transaksi, sedangkan DAO serta controller tetap menjalankan fungsi sebelumnya.
