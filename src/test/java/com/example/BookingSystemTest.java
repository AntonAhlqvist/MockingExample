package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
     * Testar att ett IllegalArgumentException kastas när ogiltiga bokningstider anges.
     * <p>
     * Metoden mockar först den aktuella tiden via mockCurrentTime() och konfigurerar
     * RoomRepository att returnera ett mockat rum för det angivna rums-id:t.
     * Den kör sedan bookRoom() med olika kombinationer av start- och sluttider
     * som är ogiltiga.
     * <p>
     * Testet kontrollerar att bookRoom() kastar IllegalArgumentException för:
     * starttid i dåtid, sluttid före starttid eller sluttid som är lika med starttid.
     * <p>
     * Detta säkerställer att bokningssystemet korrekt hanterar ogiltiga tidsintervall
     * och inte tillåter bokningar som bryter mot affärsreglerna.
     */
    @ParameterizedTest(name = "Start={0}, End={1} ska ge IllegalArgumentException")
    @MethodSource("invalidBookingTimes")
    void shouldThrowForInvalidBookingTimes(LocalDateTime start,
                                           LocalDateTime end) {
        mockCurrentTime();

        when(roomRepository.findById("Rum 1"))
                .thenReturn(Optional.of(room));

        assertThrows(IllegalArgumentException.class,
                () -> bookingSystem.bookRoom("Rum 1", start, end));
    }

    /**
     * Provider för ogiltiga bokningstider som används i shouldThrowForInvalidBookingTimes().
     * <p>
     * Streamen innehåller exempel på starttider och sluttider som inte är tillåtna:
     * starttid i dåtid, sluttid före starttid samt sluttid lika med starttid.
     * <p>
     * Dessa värden används av det parameteriserade testet för att säkerställa att
     * bookRoom() korrekt kastar IllegalArgumentException vid ogiltiga bokningstider.
     */
    private static Stream<Arguments> invalidBookingTimes() {

        LocalDateTime now =
                LocalDateTime.of(2026, 2, 1, 10, 0);

        return Stream.of(

                Arguments.of(
                        now.minusMinutes(1),
                        now.plusHours(1)
                ),

                Arguments.of(
                        now.plusHours(2),
                        now.plusHours(1)
                ),

                Arguments.of(
                        now.plusHours(1),
                        now.plusHours(1)
                )
        );
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

    /**
     * Testar att en bokning kan avbokas när den existerar och ännu inte har påbörjats.
     * <p>
     * Metoden mockar ett rum som innehåller en framtida bokning med angivet boknings-id.
     * TimeProvider mockas till "nu" för att säkerställa att bokningen ligger i framtiden.
     * <p>
     * Testet verifierar att cancelBooking() returnerar true, att bokningen tas bort,
     * att rummet sparas i repositoryt samt att en avbokningsbekräftelse skickas.
     */
    @Test
    void shouldCancelBookingSuccessfully() throws NotificationException {
        mockCurrentTime();

        String bookingId = "booking-123";

        Booking booking = mock(Booking.class);
        when(booking.getStartTime()).thenReturn(start);

        when(room.hasBooking(bookingId)).thenReturn(true);
        when(room.getBooking(bookingId)).thenReturn(booking);

        when(roomRepository.findAll()).thenReturn(List.of(room));

        boolean result = bookingSystem.cancelBooking(bookingId);

        assertThat(result).isTrue();

        verify(room).removeBooking(bookingId);
        verify(roomRepository).save(room);
        verify(notificationService).sendCancellationConfirmation(booking);
    }

    /**
     * Testar att cancelBooking() returnerar false när ingen bokning med angivet id finns.
     * <p>
     * RoomRepository mockas att returnera ett rum som inte innehåller bokningen.
     * Eftersom ingen matchande bokning hittas ska metoden avslutas tidigt
     * utan att spara eller skicka notifiering.
     */
    @Test
    void shouldReturnFalseWhenBookingDoesNotExist() throws NotificationException {

        String bookingId = "missing-booking";

        when(room.hasBooking(bookingId)).thenReturn(false);
        when(roomRepository.findAll()).thenReturn(List.of(room));

        boolean result = bookingSystem.cancelBooking(bookingId);

        assertThat(result).isFalse();

        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendCancellationConfirmation(any());
    }

    /**
     * Testar att cancelBooking() kastar IllegalStateException
     * när bokningen redan har påbörjats.
     * <p>
     * Bokningens starttid sätts till före "nu", vilket innebär
     * att den inte längre får avbokas enligt affärsreglerna.
     * <p>
     * Testet verifierar att ingen borttagning, sparning eller
     * notifiering sker när avbokningen nekas.
     */
    @Test
    void shouldThrowIfCancellingStartedBooking() throws NotificationException {

        mockCurrentTime();

        String bookingId = "started-booking";

        Booking booking = mock(Booking.class);

        when(booking.getStartTime()).thenReturn(now.minusMinutes(10));

        when(room.hasBooking(bookingId)).thenReturn(true);
        when(room.getBooking(bookingId)).thenReturn(booking);

        when(roomRepository.findAll()).thenReturn(List.of(room));

        assertThrows(IllegalStateException.class,
                () -> bookingSystem.cancelBooking(bookingId));

        verify(room, never()).removeBooking(any());
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendCancellationConfirmation(any());
    }

    /**
     * Testar att ett IllegalArgumentException kastas när någon av parametrarna är null.
     * <p>
     * Metoden mockar först den aktuella tiden via mockCurrentTime().
     * Den kör sedan bookRoom() med olika kombinationer av starttid, sluttid och rum-id
     * där minst en av parametrarna är null.
     * <p>
     * Testet kontrollerar att bookRoom() kastar IllegalArgumentException när
     * starttid, sluttid eller rum-id saknas.
     * <p>
     * Detta säkerställer att bokningssystemet inte tillåter bokningar med
     * ogiltiga eller ofullständiga parametrar och följer affärsreglerna.
     */
    @ParameterizedTest(name = "Start={0}, End={1}, RoomId={2} ska ge IllegalArgumentException")
    @MethodSource("nullBookingArguments")
    void shouldThrowForNullArguments(LocalDateTime start, LocalDateTime end, String roomId) {
        mockCurrentTime();

        assertThrows(IllegalArgumentException.class,
                () -> bookingSystem.bookRoom(roomId, start, end));
    }

    /**
     * Provider för null-parametrar som används i shouldThrowForNullArguments().
     * <p>
     * Streamen innehåller exempel där starttid, sluttid eller rum-id är null.
     * <p>
     * Dessa värden används av det parameteriserade testet för att säkerställa att
     * bookRoom() korrekt kastar IllegalArgumentException när någon parameter saknas.
     */
    private static Stream<Arguments> nullBookingArguments() {
        LocalDateTime validStart = LocalDateTime.of(2026, 2, 1, 11, 0);
        LocalDateTime validEnd = validStart.plusHours(1);

        return Stream.of(
                Arguments.of(null, validEnd, "Rum 1"),   // null start
                Arguments.of(validStart, null, "Rum 1"), // null end
                Arguments.of(validStart, validEnd, null) // null roomId
        );
    }
}
