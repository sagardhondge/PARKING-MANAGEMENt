import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class ParkingManagement extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/parking_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Sagar@9075";

    public ParkingManagement() {
        setTitle("Parking Management System");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 1, 5, 5));

        JButton btnEntry = new JButton("Vehicle Entry");
        JButton btnExit = new JButton("Vehicle Exit");
        JButton btnSlots = new JButton("View Parking Slots");

        // Customize buttons
        customizeButton(btnEntry, new Color(0, 150, 136), Color.WHITE);
        customizeButton(btnExit, new Color(255, 87, 34), Color.WHITE);
        customizeButton(btnSlots, new Color(63, 81, 181), Color.WHITE);

        btnEntry.addActionListener(e -> new EntryForm());
        btnExit.addActionListener(e -> new ExitForm());
        btnSlots.addActionListener(e -> new SlotViewer());

        add(btnEntry);
        add(btnExit);
        add(btnSlots);

        setVisible(true);
    }

    private void customizeButton(JButton button, Color bgColor, Color fgColor) {
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        new ParkingManagement();
    }

    class EntryForm extends JFrame {
        private JTextField txtVehicleNumber;

        public EntryForm() {
            setTitle("Vehicle Entry");
            setSize(300, 200);
            setLayout(new GridLayout(3, 2, 10, 10));

            JLabel lblVehicleNumber = new JLabel("Vehicle Number:");
            lblVehicleNumber.setFont(new Font("Arial", Font.BOLD, 12));
            add(lblVehicleNumber);

            txtVehicleNumber = new JTextField();
            add(txtVehicleNumber);

            JButton btnSubmit = new JButton("Submit");
            customizeButton(btnSubmit, new Color(175, 162, 76), Color.WHITE);
            add(btnSubmit);

            btnSubmit.addActionListener(e -> registerVehicle());

            setVisible(true);
        }

        private void registerVehicle() {
            String vehicleNumber = txtVehicleNumber.getText().trim();
            if (vehicleNumber.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a vehicle number!");
                return;
            }

            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO vehicles (vehicle_number) VALUES (?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, vehicleNumber);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Vehicle " + vehicleNumber + " added!");
                dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    class ExitForm extends JFrame {
        private JTextField txtVehicleNumber;

        public ExitForm() {
            setTitle("Vehicle Exit");
            setSize(350, 250);
            setLayout(new GridLayout(4, 2, 10, 10));

            JLabel lblVehicleNumber = new JLabel("Vehicle Number:");
            lblVehicleNumber.setFont(new Font("Arial", Font.BOLD, 12));
            add(lblVehicleNumber);

            txtVehicleNumber = new JTextField();
            add(txtVehicleNumber);

            JButton btnCheckout = new JButton("Checkout");
            JButton btnReceipt = new JButton("Generate Receipt");

            customizeButton(btnCheckout, new Color(255, 87, 34), Color.WHITE);
            customizeButton(btnReceipt, new Color(63, 81, 181), Color.WHITE);

            add(btnCheckout);
            add(btnReceipt);

            btnCheckout.addActionListener(e -> processExit());
            btnReceipt.addActionListener(e -> generateReceipt());

            setVisible(true);
        }

        private void processExit() {
            String vehicleNumber = txtVehicleNumber.getText().trim();
            if (vehicleNumber.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a vehicle number!");
                return;
            }

            try (Connection conn = getConnection()) {
                String fetchSql = "SELECT entry_time FROM vehicles WHERE vehicle_number = ? AND exit_time IS NULL";
                PreparedStatement fetchStmt = conn.prepareStatement(fetchSql);
                fetchStmt.setString(1, vehicleNumber);
                ResultSet rs = fetchStmt.executeQuery();

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Vehicle not found or already exited!");
                    return;
                }

                Timestamp entryTime = rs.getTimestamp("entry_time");
                LocalDateTime exitTime = LocalDateTime.now();

                long hours = Duration.between(entryTime.toLocalDateTime(), exitTime).toHours();
                double fee = hours > 0 ? hours * 10.0 : 10.0;

                String updateSql = "UPDATE vehicles SET exit_time = ?, fee = ? WHERE vehicle_number = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setTimestamp(1, Timestamp.valueOf(exitTime));
                updateStmt.setDouble(2, fee);
                updateStmt.setString(3, vehicleNumber);
                updateStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Vehicle " + vehicleNumber + " exited. Fee: ₹" + fee);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }

        private void generateReceipt() {
            String vehicleNumber = txtVehicleNumber.getText().trim();
            if (vehicleNumber.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a vehicle number to generate receipt!");
                return;
            }

            try (Connection conn = getConnection()) {
                String fetchSql = "SELECT entry_time, exit_time, fee FROM vehicles WHERE vehicle_number = ?";
                PreparedStatement stmt = conn.prepareStatement(fetchSql);
                stmt.setString(1, vehicleNumber);
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "No record found for this vehicle!");
                    return;
                }

                Timestamp entryTime = rs.getTimestamp("entry_time");
                Timestamp exitTime = rs.getTimestamp("exit_time");
                double fee = rs.getDouble("fee");

                String receipt = "====== Parking Receipt ======\n" +
                        "Vehicle Number: " + vehicleNumber + "\n" +
                        "Entry Time: " + entryTime + "\n" +
                        "Exit Time: " + exitTime + "\n" +
                        "Total Fee: ₹" + fee + "\n" +
                        "============================";

                JTextArea receiptArea = new JTextArea(receipt);
                receiptArea.setEditable(false);
                receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JOptionPane.showMessageDialog(this, new JScrollPane(receiptArea), "Parking Receipt", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    class SlotViewer extends JFrame {
        public SlotViewer() {
            setTitle("Parking Slots");
            setSize(400, 300);
            setLayout(new BorderLayout());

            JTextArea slotInfo = new JTextArea();
            slotInfo.setEditable(false);
            slotInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
            add(new JScrollPane(slotInfo), BorderLayout.CENTER);

            try (Connection conn = getConnection()) {
                String sql = "SELECT slot_number, status FROM parking_slots";
                String countSql = "SELECT " +
                        "COUNT(CASE WHEN status = 'Available' THEN 1 END) AS available_slots, " +
                        "COUNT(CASE WHEN status = 'Occupied' THEN 1 END) AS occupied_slots " +
                        "FROM parking_slots";

                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                StringBuilder data = new StringBuilder("Slot Status:\n");
                while (rs.next()) {
                    int slot = rs.getInt("slot_number");
                    String status = rs.getString("status");
                    data.append("Slot ").append(slot).append(": ").append(status).append("\n");
                }

                PreparedStatement countStmt = conn.prepareStatement(countSql);
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    int available = countRs.getInt("available_slots");
                    int occupied = countRs.getInt("occupied_slots");
                    data.append("\nTotal Slots: ").append(available + occupied)
                            .append("\nAvailable Slots: ").append(available)
                            .append("\nOccupied Slots: ").append(occupied);
                }

                slotInfo.setText(data.toString());
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }

            setVisible(true);
        }
    }
}