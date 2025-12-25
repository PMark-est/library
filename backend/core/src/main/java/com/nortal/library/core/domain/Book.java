package com.nortal.library.core.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
public class Book {

  @Id private String id;

  @Column(nullable = false)
  private String title;

  @ManyToOne
  @JoinColumn(name = "loaned_to_member_id")
  private Member loanedTo;

  @ManyToMany
  @JoinTable(
          name = "book_reservations",
          joinColumns = @JoinColumn(name = "book_id"),
          inverseJoinColumns = @JoinColumn(name = "member_id")
  )
  @OrderColumn(name = "position")
  private List<Member> reservationQueue = new ArrayList<>();

  @Column(name = "due_date")
  private LocalDate dueDate;

  public Book(String id, String title) {
    this.id = id;
    this.title = title;
  }
}
