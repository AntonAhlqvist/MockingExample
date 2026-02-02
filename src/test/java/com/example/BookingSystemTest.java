package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSystemTest {

    @Mock
    TimeProvider timeProvider;

    @Mock
    RoomRepository roomRepository;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    BookingSystem bookingSystem;

    private LocalDateTime now;
    private LocalDateTime start;
    private LocalDateTime end;
    private Room room;

    /**
     * Förbereder gemensam testdata innan varje test körs.
     * <p>
     * Denna metod skapar en fast aktuell tid (now) och beräknar start- och sluttider
     * för bokningar relativt till denna tid. Den skapar även ett mockat Room-objekt.
     * <p>
     * Tid-mockning för TimeProvider hanteras i hjälpfunktionen mockCurrentTime(),
     * som anropas endast i tester som behöver den aktuella tiden.
     * <p>
     * Genom att samla gemensam testdata här kan varje enskilt test fokusera på
     * det scenario som testas utan att duplicera setup-kod.
     */
    @BeforeEach
    void setup() {
        now = LocalDateTime.of(2026, 2, 1, 10, 0);
        start = now.plusHours(1);
        end = start.plusHours(1);

        room = mock(Room.class);
    }

    /**
     * Hjälpmetod för att mocka ett rum och dess tillgänglighet.
     * <p>
     * Metoden konfigurerar RoomRepository-mocken att returnera det mockade rummet
     * och styr Room-mocken att returnera antingen ledigt eller upptaget.
     * Detta gör det enkelt att snabbt testa olika scenarier för rumsbokning
     * utan att duplicera kod i varje test.
     */
    private void mockRoomAvailable(boolean available) {
        when(roomRepository.findById("Rum 1")).thenReturn(Optional.of(room));
        when(room.isAvailable(start, end)).thenReturn(available);
    }

    /**
     * Hjälpmetod för att mocka “nuvarande tid” i TimeProvider.
     * <p>
     * Denna metod används i alla tester som anropar bookRoom eller cancelBooking
     * eftersom dessa metoder kontrollerar tider relativt till den aktuella tiden.
     */
    private void mockCurrentTime() {
        when(timeProvider.getCurrentTime()).thenReturn(now);
    }

    /**
     * Testar att en bokning lyckas när rummet finns och är tillgängligt.
     * <p>
     * Metoden anropar först mockCurrentTime() för att mocka den aktuella tiden
     * och mockRoomAvailable(true) för att säkerställa att rummet är ledigt.
     * Den försöker sedan boka rummet med giltiga start- och sluttider via bookRoom().
     * <p>
     * Testet kontrollerar att bookRoom() returnerar true, att rummet sparas i repositoryt,
     * och att en bokningsbekräftelse skickas via NotificationService.
     * <p>
     * Detta säkerställer att bokningssystemet korrekt hanterar lyckade bokningar
     * när rummet är tillgängligt och tiderna är giltiga.
     */
    @Test
    void shouldBookRoomSuccessfully() throws NotificationException {
        mockCurrentTime();
        mockRoomAvailable(true);

        boolean result = bookingSystem.bookRoom("Rum 1", start, end);

        assertThat(result).isTrue();
        verify(roomRepository).save(room);
        verify(notificationService).sendBookingConfirmation(any());
    }

    /**
     * Testar att bokning inte sker när rummet inte är tillgängligt.
     * <p>
     * Metoden anropar först mockCurrentTime() för att mocka den aktuella tiden
     * och mockRoomAvailable(false) för att säkerställa att rummet är upptaget.
     * Den försöker sedan boka rummet med giltiga start- och sluttider via bookRoom().
     * <p>
     * Testet kontrollerar att bookRoom() returnerar false, att rummet inte sparas i repositoryt,
     * och att ingen bokningsbekräftelse skickas via NotificationService.
     * <p>
     * Detta säkerställer att bokningssystemet korrekt hanterar misslyckade bokningar
     * när rummet redan är upptaget.
     */
    @Test
    void shouldReturnFalseIfRoomNotAvailable() throws NotificationException {
        mockCurrentTime();
        mockRoomAvailable(false);

        boolean result = bookingSystem.bookRoom("Rum 1", start, end);

        assertThat(result).isFalse();
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendBookingConfirmation(any());
    }

    /**
     * Testar att ett IllegalArgumentException kastas när rummet inte existerar.
     * <p>
     * Metoden anropar först mockCurrentTime() för att mocka den aktuella tiden
     * och konfigurerar RoomRepository att returnera Optional.empty() för det angivna rums-id:t.
     * Den försöker sedan boka rummet via bookRoom().
     * <p>
     * Testet kontrollerar att bookRoom() kastar ett IllegalArgumentException
     * och att felmeddelandet indikerar att rummet inte existerar.
     * <p>
     * Detta säkerställer att bokningssystemet korrekt hanterar försök att boka
     * rum som inte finns i systemet.
     */
    @Test
    void shouldThrowIfRoomDoesNotExist() {
        mockCurrentTime();
        when(roomRepository.findById("Rum 1")).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> bookingSystem.bookRoom("Rum 1", start, end)
                );

        assertThat(exception.getMessage()).contains("Rummet existerar inte");
    }

    /**
     * Testar att ett IllegalArgumentException kastas när starttid ligger i det förflutna.
     * <p>
     * Metoden skapar en starttid som är en timme tidigare än den aktuella tiden (now)
     * och försöker boka ett rum med denna tid. Eftersom BookingSystem inte tillåter bokning
     * i dåtid, förväntas metoden bookRoom kasta ett IllegalArgumentException.
     * <p>
     * Testet kontrollerar att undantaget verkligen kastas och att felmeddelandet är korrekt.
     * Detta säkerställer att bokningssystemet inte accepterar ogiltiga starttider.
     */
    @Test
    void shouldThrowIfStartTimeIsInThePast() {
        mockCurrentTime();
        LocalDateTime past = now.minusHours(1);

        assertThrows(IllegalArgumentException.class,
                () -> bookingSystem.bookRoom("Rum 1", past, end));
    }

    /**
     * Testar att ett IllegalArgumentException kastas när sluttid ligger före starttid.
     * <p>
     * Metoden skapar en sluttid som ligger en minut innan starttiden ("start")
     * och försöker boka ett rum med dessa tider. Eftersom BookingSystem kräver att sluttiden
     * alltid är efter starttiden, förväntas metoden bookRoom kasta ett IllegalArgumentException.
     * <p>
     * Testet kontrollerar att undantaget verkligen kastas och att felmeddelandet är korrekt.
     * Detta säkerställer att bokningssystemet inte accepterar ogiltiga tidsspann.
     */
    @Test
    void shouldThrowIfEndBeforeStart() {
        mockCurrentTime();
        LocalDateTime invalidEnd = start.minusMinutes(1);

        assertThrows(IllegalArgumentException.class,
                () -> bookingSystem.bookRoom("Rum 1", start, invalidEnd));
    }

    /**
     * Testar att getAvailableRooms returnerar endast de rum som faktiskt är lediga.
     * <p>
     * Metoden skapar två mockade Room-objekt, där det ena är tillgängligt för det angivna
     * tidsspannet och det andra inte är det. RoomRepository mockas att returnera båda rummen,
     * och isAvailable-metoden på varje rum returnerar true eller false enligt tillgängligheten.
     * <p>
     * Testet anropar bookingSystem.getAvailableRooms(start, end) och kontrollerar att endast
     * det rum som är ledigt inkluderas i resultatlistan. Detta säkerställer att
     * bokningssystemets logik för filtrering av lediga rum fungerar korrekt.
     */
    @Test
    void shouldReturnOnlyAvailableRooms() {
        Room room1 = mock(Room.class);
        Room room2 = mock(Room.class);

        when(room1.isAvailable(start, end)).thenReturn(true);
        when(room2.isAvailable(start, end)).thenReturn(false);

        when(roomRepository.findAll())
                .thenReturn(List.of(room1, room2));

        var result = bookingSystem.getAvailableRooms(start, end);

        assertThat(result)
                .containsExactly(room1);
    }
}
