package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A parent/guardian record, independent of any login account -- see
 *  docs/PARENT_PORTAL_DESIGN_PROPOSAL.md. Deliberately its own entity rather than reusing
 *  {@code Student.fatherEmail}/{@code motherEmail}: those are per-student free text with no
 *  uniqueness constraint, so the same real person entered slightly differently across two
 *  siblings' records could never be recognized as one guardian. {@code email} is unique here
 *  and doubles as the Keycloak login-matching key once a login account is provisioned. */
@Entity
@Table(name = "guardians")
@EntityListeners(AuditingEntityListener.class)
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    /** Free text for display only, e.g. "Father"/"Mother"/"Guardian" -- not used for any
     *  access-control decision. */
    @Column(name = "relationship_hint", length = 50)
    private String relationshipHint;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRelationshipHint() { return relationshipHint; }
    public void setRelationshipHint(String relationshipHint) { this.relationshipHint = relationshipHint; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() { return firstName + " " + lastName; }
}
