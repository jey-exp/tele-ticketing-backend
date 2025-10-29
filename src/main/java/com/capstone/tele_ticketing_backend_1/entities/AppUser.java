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
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users") // The table name in the DB remains "users"
@Data
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();


    // Self-referencing ManyToMany for team structure
    @ManyToMany
    @JoinTable(
            name = "team_memberships",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "team_lead_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<AppUser> teamLeads = new HashSet<>();

    @ManyToMany(mappedBy = "teamLeads")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<AppUser> teamMembers = new HashSet<>();

    // A user can be assigned to many tickets as an engineer
    @ManyToMany(mappedBy = "assignedEngineers")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Ticket> assignedTickets = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AppUser(String username, String password, String fullName ){
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }
}
