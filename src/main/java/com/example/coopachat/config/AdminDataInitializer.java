package com.example.coopachat.config;

import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.default.email:admincoopachat@yopmail.com}")
    private String adminEmail;

    @Value("${admin.default.password:Passer@123}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        // Vérifier si un admin existe déjà
        if ( utilisateurRepository.findByRole (UserRole.ADMINISTRATOR).isEmpty()){

            Users admin = new Users ();
            admin.setRole (UserRole.ADMINISTRATOR);
            admin.setFirstName("Admin");
            admin.setLastName("Super");
            admin.setEmail(adminEmail);
            admin.setPhone("+221 77 000 00 00");
            admin.setIsActive(true);
            admin.setPassword(passwordEncoder.encode(adminPassword));

            utilisateurRepository.save(admin);

            System.out.println("✅ Administrateur par défaut créé avec succès !");
            System.out.println("📧 Email: "+adminEmail);
            System.out.println("🔑 Mot de passe: "+adminPassword);
        } else {
            System.out.println("ℹ️ Un administrateur existe déjà");
        }



    }

}

