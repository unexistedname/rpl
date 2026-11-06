package bobob.model;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    private static SessionFactory build() {
        Configuration cfg = new Configuration();

        cfg.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
        cfg.setProperty("hibernate.connection.url", Database.URL);
        cfg.setProperty("hibernate.dialect",
            "org.hibernate.community.dialect.SQLiteDialect");
        cfg.setProperty("hibernate.hbm2ddl.auto", "none");
        cfg.setProperty("hibernate.show_sql", "false");
        cfg.setProperty("hibernate.format_sql", "false");

        cfg.addAnnotatedClass(Kategori.class);
        cfg.addAnnotatedClass(Barang.class);
        cfg.addAnnotatedClass(Transaksi.class);
        cfg.addAnnotatedClass(Distributor.class);
        cfg.addAnnotatedClass(Pengadaan.class);
        cfg.addAnnotatedClass(Cashflow.class);

        return cfg.buildSessionFactory();
    }

    public static synchronized void init() {
        if (sessionFactory == null) {
            sessionFactory = build();
        }
    }

    public static Session open() {
        init();
        return sessionFactory.openSession();
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
