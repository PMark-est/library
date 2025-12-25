package com.nortal.library.core;

import com.nortal.library.core.domain.Book;
import com.nortal.library.core.domain.Member;
import com.nortal.library.core.port.BookRepository;
import com.nortal.library.core.port.MemberRepository;
import java.time.LocalDate;
import java.util.*;

public class LibraryService {
  private static final int MAX_LOANS = 5;
  private static final int DEFAULT_LOAN_DAYS = 14;

  private final BookRepository bookRepository;
  private final MemberRepository memberRepository;

  public LibraryService(BookRepository bookRepository, MemberRepository memberRepository) {
    this.bookRepository = bookRepository;
    this.memberRepository = memberRepository;
  }

  public Result borrowBook(String bookId, String memberId) {
    Optional<Book> book = bookRepository.findById(bookId);
    Optional<Member> member = memberRepository.findById(memberId);
    if (book.isEmpty()) {
      return Result.failure("BOOK_NOT_FOUND");
    }
    if (member.isEmpty()) {
      return Result.failure("MEMBER_NOT_FOUND");
    }
    if (!canMemberBorrow(memberId)) {
      return Result.failure("BORROW_LIMIT");
    }

    Book entity = book.get();

    if (entity.getLoanedTo() != null) {
      return Result.failure("BOOK_BORROWED");
    }

    Member borrower = member.get();

    entity.setLoanedTo(borrower);
    entity.setDueDate(LocalDate.now().plusDays(DEFAULT_LOAN_DAYS));
    bookRepository.save(entity);

    borrower.getLoanedBooks().add(entity);


    return Result.success();
  }

  public ResultWithNext returnBook(String bookId, String memberId) {
    Optional<Book> book = bookRepository.findById(bookId);
    Optional<Member> member = memberRepository.findById(memberId);

    if (book.isEmpty()) {
      return ResultWithNext.failure();
    }

    if (member.isEmpty()){
        return ResultWithNext.failure();
    }

    Book entity = book.get();

    if (!Objects.equals(entity.getLoanedTo().getId(), memberId)){
        return ResultWithNext.failure();
    }


    Member nextMember = null;


    if (entity.getReservationQueue().isEmpty()){
        entity.setLoanedTo(null);
        entity.setDueDate(null);
    } else {
        nextMember = entity.getReservationQueue().stream()
                .filter(this::canMemberBorrow)
                .findFirst()
                .orElse(null);

    }

    Member borrower = member.get();
    borrower.getLoanedBooks().remove(entity);

    bookRepository.save(entity);

    if (nextMember == null){
      return ResultWithNext.success(null);
    }

    return ResultWithNext.success(nextMember.getId());
  }

  public Result reserveBook(String bookId, String memberId) {
    Optional<Book> book = bookRepository.findById(bookId);
    Optional<Member> member = memberRepository.findById(memberId);

    if (book.isEmpty()) {
      return Result.failure("BOOK_NOT_FOUND");
    }
    if (member.isEmpty()) {
      return Result.failure("MEMBER_NOT_FOUND");
    }

    Book entity = book.get();
    Member borrower = member.get();

    if (entity.getReservationQueue().isEmpty() & canMemberBorrow(memberId)){
        return borrowBook(bookId, memberId);
    }

    if (entity.getReservationQueue().contains(borrower)){
        return Result.failure("DUPLICATE_RESERVATION");
    }

    entity.getReservationQueue().add(borrower);
    bookRepository.save(entity);
    return Result.success();
  }

  public Result cancelReservation(String bookId, String memberId) {
    Optional<Book> book = bookRepository.findById(bookId);
    Optional<Member> member = memberRepository.findById(memberId);

    if (book.isEmpty()) {
      return Result.failure("BOOK_NOT_FOUND");
    }
    if (member.isEmpty()) {
      return Result.failure("MEMBER_NOT_FOUND");
    }

    Book entity = book.get();
    Member borrower = member.get();
    boolean removed = entity.getReservationQueue().remove(borrower);
    if (!removed) {
      return Result.failure("NOT_RESERVED");
    }
    bookRepository.save(entity);
    return Result.success();
  }

  public boolean canMemberBorrow(String memberId) {
    Optional<Member> member = memberRepository.findById(memberId);

    if (member.isEmpty()) {
      return false;
    }

    Member borrower = member.get();
    Set<Book> books = borrower.getLoanedBooks();

    return books.size() < MAX_LOANS;
  }

    public boolean canMemberBorrow(Member member) {
      if (member == null){
          return false;
      }
      Set<Book> books = member.getLoanedBooks();

      return books.size() < MAX_LOANS;
    }

  public List<Book> searchBooks(String titleContains, Boolean availableOnly, String loanedTo) {
    return bookRepository.findAll().stream()
        .filter(
            b ->
                titleContains == null
                    || b.getTitle().toLowerCase().contains(titleContains.toLowerCase()))
        .filter(b -> loanedTo == null || loanedTo.equals(b.getLoanedTo().getId()))
        .filter(
            b ->
                availableOnly == null
                    || (availableOnly ? b.getLoanedTo() == null : b.getLoanedTo() != null))
        .toList();
  }

  public List<Book> overdueBooks(LocalDate today) {
    return bookRepository.findAll().stream()
        .filter(b -> b.getLoanedTo() != null)
        .filter(b -> b.getDueDate() != null && b.getDueDate().isBefore(today))
        .toList();
  }

  public Result extendLoan(String bookId, int days) {
    if (days == 0) {
      return Result.failure("INVALID_EXTENSION");
    }
    Optional<Book> book = bookRepository.findById(bookId);
    if (book.isEmpty()) {
      return Result.failure("BOOK_NOT_FOUND");
    }
    Book entity = book.get();
    if (entity.getLoanedTo() == null) {
      return Result.failure("NOT_LOANED");
    }
    LocalDate baseDate =
        entity.getDueDate() == null
            ? LocalDate.now().plusDays(DEFAULT_LOAN_DAYS)
            : entity.getDueDate();
    entity.setDueDate(baseDate.plusDays(days));
    bookRepository.save(entity);
    return Result.success();
  }

  public MemberSummary memberSummary(String memberId) {
    Optional<Member> member = memberRepository.findById(memberId);

    if (member.isEmpty()) {
      return new MemberSummary(false, "MEMBER_NOT_FOUND", List.of(), List.of());
    }

    Member borrower = member.get();
    Set<Book> loanedBooks = borrower.getLoanedBooks();
    Set<Book> reservedBooks = borrower.getReservedBooks();
    List<ReservationPosition> reservations = new ArrayList<>();
    for (Book book : reservedBooks) {
      int idx = book.getReservationQueue().indexOf(borrower);
      if (idx >= 0) {
        reservations.add(new ReservationPosition(book.getId(), idx));
      }
    }
    return new MemberSummary(true, null, loanedBooks.stream().toList(), reservations);
  }

  public Optional<Book> findBook(String id) {
    return bookRepository.findById(id);
  }

  public List<Book> allBooks() {
    return bookRepository.findAll();
  }

  public List<Member> allMembers() {
    return memberRepository.findAll();
  }

  public Result createBook(String id, String title) {
    if (id == null || title == null) {
      return Result.failure("INVALID_REQUEST");
    }
    bookRepository.save(new Book(id, title));
    return Result.success();
  }

  public Result updateBook(String id, String title) {
    Optional<Book> existing = bookRepository.findById(id);
    if (existing.isEmpty()) {
      return Result.failure("BOOK_NOT_FOUND");
    }
    if (title == null) {
      return Result.failure("INVALID_REQUEST");
    }
    Book book = existing.get();
    book.setTitle(title);
    bookRepository.save(book);
    return Result.success();
  }

  public Result deleteBook(String id) {
    Optional<Book> existing = bookRepository.findById(id);
    if (existing.isEmpty()) {
      return Result.failure("BOOK_NOT_FOUND");
    }
    Book book = existing.get();

    Member borrower = book.getLoanedTo();
    borrower.getLoanedBooks().remove(book);
    memberRepository.save(borrower);

    bookRepository.delete(book);
    return Result.success();
  }

  public Result createMember(String id, String name) {
    if (id == null || name == null) {
      return Result.failure("INVALID_REQUEST");
    }
    memberRepository.save(new Member(id, name));
    return Result.success();
  }

  public Result updateMember(String id, String name) {
    Optional<Member> existing = memberRepository.findById(id);
    if (existing.isEmpty()) {
      return Result.failure("MEMBER_NOT_FOUND");
    }
    if (name == null) {
      return Result.failure("INVALID_REQUEST");
    }
    Member member = existing.get();
    member.setName(name);
    memberRepository.save(member);
    return Result.success();
  }

  public Result deleteMember(String id) {
    Optional<Member> existing = memberRepository.findById(id);
    if (existing.isEmpty()) {
      return Result.failure("MEMBER_NOT_FOUND");
    }

    Set<Book> books = existing.get().getLoanedBooks();
    Member nextMember;
    for (Book book: books){
        nextMember = book.getReservationQueue().stream()
                .filter(this::canMemberBorrow)
                .findFirst()
                .orElse(null);

        book.setLoanedTo(nextMember);

        bookRepository.save(book);
    }

    memberRepository.delete(existing.get());
    return Result.success();
  }

  public record Result(boolean ok, String reason) {
    public static Result success() {
      return new Result(true, null);
    }

    public static Result failure(String reason) {
      return new Result(false, reason);
    }
  }

  public record ResultWithNext(boolean ok, String nextMemberId) {
    public static ResultWithNext success(String nextMemberId) {
      return new ResultWithNext(true, nextMemberId);
    }

    public static ResultWithNext failure() {
      return new ResultWithNext(false, null);
    }
  }

  public record MemberSummary(
      boolean ok, String reason, List<Book> loans, List<ReservationPosition> reservations) {}

  public record ReservationPosition(String bookId, int position) {}
}
