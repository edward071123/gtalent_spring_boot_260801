package student.ed.gtalent_spring_boot_260801.repository;

import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Book;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;


import java.util.ArrayList;
import java.util.List;

@Repository
public class BookRepositoryImpl implements BookRepository {

    // EntityManager 是 JPA 操作資料庫的核心工具，可以查詢、新增、更新、刪除 Entity。
    // @PersistenceContext 會請 Spring / JPA 注入目前 persistence context 對應的 EntityManager。
    @PersistenceContext
    private EntityManager entityManager;

    private final PlatformTransactionManager transactionManager;

    public BookRepositoryImpl(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public List<Book> findAll() {
        List<?> queryResults =  entityManager
                                .createNativeQuery("SELECT * FROM books", Book.class)
                                .getResultList();

        List<Book> books = new ArrayList<>();                        
        for(Object obj : queryResults) {
            books.add((Book) obj);
        }

        return books;
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
}
