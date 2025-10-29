package com.capstone.tele_ticketing_backend_1.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_uid", unique = true, length = 50)
    private String ticketUid;

    @Column(nullable = false)
    private String title;

    @Lob // Specifies that this should be mapped to a large object type in the DB
    private String description;

    @Column(length = 50)
    private String status;

    @Column(length = 50)
    private String priority;

    @Column(length = 50)
    private String severity;

    @Column(nullable = false)
    private String category;

    @Column(name = "sub_category", nullable = false)
    private String subCategory;

    @Column(name = "sla_duration_hours")
    private Integer slaDurationHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;

    // Relationship to assigned engineers (many-to-many)
    @ManyToMany
    @JoinTable(
            name = "ticket_assignments",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "engineer_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<AppUser> assignedEngineers = new HashSet<>();

    // Relationship to activities (one-to-many)
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<TicketActivity> activities = new HashSet<>();

    // Relationship to attachments (one-to-many)
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Attachment> attachments = new HashSet<>();

    // Relationship to feedback (one-to-one)
    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Feedback feedback;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PostPersist
    private void onPostPersist() {
        if (this.ticketUid == null) {
            this.ticketUid = "TK" + (this.id + 1000);
        }
    }
}