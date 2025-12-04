package br.com.core.barbershop.domain;

import java.util.List;
import java.util.UUID;


import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank(message = "The username cannot be empty")
    @Column(name = "usernames", nullable = false)
    private String username;

    @Email(message = "It must be on email format")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "the cellphone number cannot be empty")
    @Column(name = "cellphones",nullable = false)
    private String cellPhone;
    
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Appointment> histories;

    private String address;



}
