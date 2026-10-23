package com.challenge.eventstream.config;

import com.challenge.eventstream.model.Event;
import com.challenge.eventstream.model.User;
import com.challenge.eventstream.queue.InMemoryEventQueue;
import com.challenge.eventstream.repository.EventRepository;
import com.challenge.eventstream.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final InMemoryEventQueue eventQueue;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           EventRepository eventRepository,
                           InMemoryEventQueue eventQueue,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventQueue = eventQueue;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default admin user if no users present
        if (userRepository.count() == 0) {
            User admin = new User("admin", passwordEncoder.encode("password123"), "ROLE_ADMIN");
            userRepository.save(admin);
            logger.info("Initialized default user: username='admin', password='password123'");
        }

        // Preload latest 500 events from database into in-memory queue
        try {
            List<Event> recentEvents = eventRepository.findTop500ByOrderByTimestampDesc();
            if (!recentEvents.isEmpty()) {
                // Invert to insert oldest first so that tail has newest
                Collections.reverse(recentEvents);
                eventQueue.addAll(recentEvents);
                logger.info("Preloaded {} events from database into in-memory queue", recentEvents.size());
            }
        } catch (Exception e) {
            logger.warn("Could not preload events into in-memory queue: {}", e.getMessage());
        }
    }
}
