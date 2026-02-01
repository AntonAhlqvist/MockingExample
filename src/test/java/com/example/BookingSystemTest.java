package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
     * Förbereder gemensamma testdata och mockar innan varje test körs.
     * <p>
     * Denna metod skapar en fast aktuell tid (now) för att kontrollera tidsberoende logik,
     * start- och sluttider för bokningar relativt till now, samt ett mockat Room-objekt.
     * Dessutom konfigureras TimeProvider-mocken att alltid returnera den fasta tiden.
     * <p>
     * Genom att samla denna setup här kan varje enskilt test fokusera på det scenario
     * som testas utan att behöva duplicera gemensam kod.
     */
    @BeforeEach
    void setup() {
        now = LocalDateTime.of(2026, 2, 1, 10, 0);
        start = now.plusHours(1);
        end = start.plusHours(1);

        room = mock(Room.class);

        when(timeProvider.getCurrentTime()).thenReturn(now);
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
     * Testar att en bokning lyckas när rummet finns och är tillgängligt.
     * <p>
     * Metoden mockar att rummet är ledigt och anropar bookRoom med giltiga start- och sluttider.
     * Den kontrollerar att metoden returnerar true, att rummet sparas i repository:t
     * och att en bokningsbekräftelse skickas via NotificationService.
     */
    @Test
    void shouldBookRoomSuccessfully() throws NotificationException {
        mockRoomAvailable(true);

        boolean result = bookingSystem.bookRoom("Rum 1", start, end);

        assertThat(result).isTrue();
        verify(roomRepository).save(room);
        verify(notificationService).sendBookingConfirmation(any());
    }

    /**
     * Testar att bokning inte sker när rummet inte är tillgängligt.
     * <p>
     * Metoden mockar att rummet är upptaget och anropar bookRoom med giltiga start- och sluttider.
     * Den kontrollerar att metoden returnerar false, att rummet inte sparas i repository:t
     * och att ingen bokningsbekräftelse skickas via NotificationService.
     */
    @Test
    void shouldReturnFalseIfRoomNotAvailable() throws NotificationException {
        mockRoomAvailable(false);

        boolean result = bookingSystem.bookRoom("Rum 1", start, end);

        assertThat(result).isFalse();
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendBookingConfirmation(any());
    }

    /**
     * Testar att ett IllegalArgumentException kastas när rummet inte existerar.
     * <p>
     * Metoden mockar RoomRepository att returnera tomt resultat och anropar bookRoom.
     * Den kontrollerar att metoden kastar IllegalArgumentException med ett meddelande
     * som indikerar att rummet inte existerar.
     */
    @Test
    void shouldThrowIfRoomDoesNotExist() {
        when(roomRepository.findById("Rum 1")).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> bookingSystem.bookRoom("Rum 1", start, end)
                );

        assertThat(exception.getMessage()).contains("Rummet existerar inte");
    }
}
