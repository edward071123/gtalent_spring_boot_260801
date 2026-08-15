package student.ed.gtalent_spring_boot_260801.repository;

import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;

import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;

@Repository
public class BookRepositoryImpl implements BookRepository {

    // EntityManager 是 JPA 操作資料庫的核心工具，可以查詢、新增、更新、刪除 Entity。
    // @PersistenceContext 會請 Spring / JPA 注入目前 persistence context 對應的 EntityManager。
    @PersistenceContext
    private EntityManager entityManager;

    private final PlatformTransactionManager transactionManager;

    private final JdbcTemplate jdbcTemplate;

    public BookRepositoryImpl(PlatformTransactionManager transactionManager, JdbcTemplate jdbcTemplate) {
        this.transactionManager = transactionManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Book> findAll(int page, int size) {
        int offset = (page - 1) * size;

        // 1代表存在, 所以要抓出status = 1
        List<?> queryResults =  entityManager
                                .createNativeQuery("SELECT * FROM books WHERE status = ? ORDER BY id ASC", Book.class)
                                .setParameter(1, 1)
                                .setFirstResult(offset)
                                .setMaxResults(size)
                                .getResultList();

        List<Book> books = new ArrayList<>();                        
        for(Object obj : queryResults) {
            books.add((Book) obj);
        }

        return books;
    }

    @Override
    public Book findOneById(Long id) {
        // 1代表存在, 所以要抓出status = 1
        Object queryResult =  entityManager
                                .createNativeQuery("SELECT * FROM books WHERE status = ? and id = ?", Book.class)
                                .setParameter(1, 1)
                                .setParameter(2, id)
                                .getSingleResult();

        return (Book) queryResult;
    }

    @Override
    public List<Book> findOneByName(String name) {
        // 1代表存在, 所以要抓出status = 1
        // SELECT * FROM books WHERE status = ? and name like '%?%';
        List<?> queryResults =  entityManager
                                .createNativeQuery("SELECT * FROM books WHERE status = ? and name like ?", Book.class)
                                .setParameter(1, 1)
                                .setParameter(2, "%" + name + "%")
                                .getResultList();

        List<Book> books = new ArrayList<>();                        
        for(Object obj : queryResults) {
            books.add((Book) obj);
        }

        return books;
    }

    @Override
    public long countAll() {
        Object queryResult =  entityManager
                                .createNativeQuery("SELECT COUNT(*) FROM books WHERE status = ?")
                                .setParameter(1, 1)
                                .getSingleResult();
        // 轉成long (因為取得的是Object, 所以必須要用Class型別去接:Number, int是基本的型別:無法承接Object)
        return ((Number) queryResult).longValue();
    }

    @Override
    public Book create(Book book) {
        // 確保交易能夠成功 => 如果新增書籍失敗，會回滾交易，避免資料庫出現不一致的狀態。   
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            // 使用 EntityManager 的 persist 方法將書籍新增到資料庫。
            entityManager.persist(book);
            // 交易成功 所以用commit 提交交易，將資料寫入資料庫。
            transactionManager.commit(status);
            return book;
        } catch (RuntimeException exception) {
            // 失敗 rollback：只要 create 過程出錯，就把這次 transaction 做過的資料庫操作取消。
            transactionManager.rollback(status);

            // 5. 統一丟資料寫入失敗，讓 GlobalExceptionHandler 判斷資料庫細項錯誤。
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception
            );
        }
        
    }

    @Override
    public void batchCreate(List<Book> books) {
        if (books.isEmpty()) {
            return;
        }

        try {
            // 大量匯入時不要呼叫 create(book) 逐筆新增。
            //
            // 如果 10 萬筆都走 create(book)，流程會變成：
            // - 開 transaction
            // - persist 一筆 Book
            // - commit
            // - 重複 10 萬次
            //
            // 這樣資料庫連線往返次數太多，效能會很差。
            //
            // batchUpdate 的概念是：
            // - SQL 樣板只寫一次：INSERT INTO books (name, price) VALUES (?, ?)
            // - 第 1 筆資料把 ? 換成第一本書的 name、price
            // - 第 2 筆資料把 ? 換成第二本書的 name、price
            // - 一整批資料交給 JDBC / 資料庫處理
            //
            // 這裡只寫入 name、price。
            // status 欄位在 migration 裡有 DEFAULT 1，所以不用手動設定。
            // deleted_at 是刪除時才會用到，新增時保持 null。
            jdbcTemplate.batchUpdate(
                    "INSERT INTO books (name, price) VALUES (?, ?)",
                    new BatchPreparedStatementSetter() {

                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            Book book = books.get(i);

                            // 第一個 ? 對應 SQL 裡的 name。
                            ps.setString(1, book.getName());

                            // 第二個 ? 對應 SQL 裡的 price。
                            ps.setInt(2, book.getPrice());
                        }

                        @Override
                        public int getBatchSize() {
                            // 告訴 JdbcTemplate：這一批總共有幾筆資料要寫入。
                            return books.size();
                        }
                    }
            );
        } catch (RuntimeException exception) {
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception
            );
        }
    }

    @Override
    public Book update(Long id, Book book) {
        // 確保交易能夠成功 => 如果新增書籍失敗，會回滾交易，避免資料庫出現不一致的狀態。   
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        Byte off = 0;
        try {
            // 先查詢資料庫中是否存在該書籍，如果不存在，則拋出例外。
            Book existingBook = entityManager.find(Book.class, id);
            if (existingBook == null) {
                throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
            }

            if(existingBook.getStatus() == off) {
                throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
            }

            existingBook.setName(book.getName());
            existingBook.setPrice(book.getPrice());
            // 交易成功 所以用commit 提交交易，將資料寫入資料庫。
            transactionManager.commit(status);
            return book;
        } catch (ResourceNotFoundException exception) {
            // 失敗 rollback：只要 update 過程出錯，就把這次 transaction 做過的資料庫操作取消。
            transactionManager.rollback(status);

            // 查不到資料不是資料庫寫入失敗，所以原樣丟出去，讓 GlobalExceptionHandler 回 400。
            throw exception;
        } catch (RuntimeException exception) {
            // 失敗 rollback：只要 create 過程出錯，就把這次 transaction 做過的資料庫操作取消。
            transactionManager.rollback(status);

            // 統一丟資料寫入失敗，讓 GlobalExceptionHandler 判斷資料庫細項錯誤。
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception
            );
        }
    }

    @Override
    public void delete(Long id) {
        // 確保交易能夠成功 => 如果刪除書籍失敗，會回滾交易，避免資料庫出現不一致的狀態。   
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        Byte off = 0;
        try {
            // 先查詢資料庫中是否存在該書籍，如果不存在，則拋出例外。
            Book existingBook = entityManager.find(Book.class, id);
            if (existingBook == null) {
                throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
            }

            if(existingBook.getStatus() == off) {
                throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
            }

            existingBook.setStatus(off);
            existingBook.setDeletedAt(LocalDateTime.now());
            // 交易成功 所以用commit 提交交易，將資料寫入資料庫。
            transactionManager.commit(status);
        } catch (ResourceNotFoundException exception) {
            // 失敗 rollback：只要 update 過程出錯，就把這次 transaction 做過的資料庫操作取消。
            transactionManager.rollback(status);

            // 查不到資料不是資料庫寫入失敗，所以原樣丟出去，讓 GlobalExceptionHandler 回 400。
            throw exception;
        } catch (RuntimeException exception) {
            // 失敗 rollback：只要 create 過程出錯，就把這次 transaction 做過的資料庫操作取消。
            transactionManager.rollback(status);

            // 統一丟資料寫入失敗，讓 GlobalExceptionHandler 判斷資料庫細項錯誤。
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception
            );
        }
    }

    
}
