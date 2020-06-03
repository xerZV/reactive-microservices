package com.simitchiyski.reservationservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

interface ReservationRepository extends ReactiveMongoRepository<Reservation, String> {

}

@EnableDiscoveryClient
@SpringBootApplication
@RequiredArgsConstructor
@EnableBinding(Sink.class)
public class ReservationServiceApplication {
    private final ReservationRepository reservationRepository;

    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceApplication.class, args);
    }

    @StreamListener
    public void process(@Input (Sink.INPUT) Flux<String> incoming) {
        incoming.map(s -> new Reservation(null, s))
                .flatMap(this.reservationRepository::save)
                .subscribe(r -> System.out.println(String.format("Saved %s with ID #%s", r.getReservationName(), r.getId())));
    }

    @Bean
    RouterFunction<ServerResponse> routes(ReservationRepository reservationRepository,
                                          Environment environment) {
        return route(GET("/reservations"), req -> ok().body(reservationRepository.findAll(), Reservation.class))
                .andRoute(GET("/message"), req -> ok().body(Flux.just(environment.getProperty("message")), String.class));
    }
}

@Component
@RequiredArgsConstructor
class DataWriter implements ApplicationRunner {

    private final ReservationRepository reservationRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        this.reservationRepository.deleteAll()
                .thenMany(Flux.just("Martin", "Bob", "Lucifer", "Keanu")
                        .map(name -> new Reservation(null, name))
                        .flatMap(this.reservationRepository::save))
                .thenMany(this.reservationRepository.findAll())
                .subscribe(System.out::println);
    }
}

//@RestController
//@RequiredArgsConstructor
//@RequestMapping(value = "/reservations") // Its not Spring MVC, its not servlet API, Its spring web flux and its Netty
//    // We are directly streaming the result, we are not collecting it into the memory and then returning it as json etc
//class ReservationRestController {
//
//    private final ReservationRepository reservationRepository;
//
//    @GetMapping
//    public Flux<Reservation> getReservations() {
//        return this.reservationRepository.findAll();
//    }
//}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
class Reservation {

    @Id
    private String id;

    private String reservationName;

}
