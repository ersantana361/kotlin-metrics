package com.metrics.model.architecture

data class DddPatternAnalysis(
    val entities: List<DddEntity>,
    val valueObjects: List<DddValueObject>,
    val services: List<DddService>,
    val repositories: List<DddRepository>,
    val aggregates: List<DddAggregate>,
    val domainEvents: List<DddDomainEvent>
)