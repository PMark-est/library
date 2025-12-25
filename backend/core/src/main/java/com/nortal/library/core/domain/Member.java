package com.nortal.library.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
public class Member {

  @Id private String id;

  @Column(nullable = false)
  private String name;

  @OneToMany(mappedBy = "loanedTo")
  private Set<Book> loanedBooks = new HashSet<>();

  @ManyToMany(mappedBy = "reservationQueue")
  private Set<Book> reservedBooks = new HashSet<>();

  public Member(String id, String name) {
    this.id = id;
    this.name = name;
  }
}
