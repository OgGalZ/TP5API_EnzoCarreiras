package com.example.tp.security;

import jakarta.servlet.Filter;

// interface commune aux deux rate limiters (maison et Bucket4j),
// on choisit lequel est actif avec app.security.rate-limit.impl
public interface LimiteurDeDebit extends Filter {
}
