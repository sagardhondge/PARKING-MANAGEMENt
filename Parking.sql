CREATE DATABASE parking_db;
USE parking_db;

-- Table for parking slots
CREATE TABLE parking_slots (
    slot_number INT AUTO_INCREMENT PRIMARY KEY,
    status ENUM('Available', 'Occupied') DEFAULT 'Available'
);

-- Table for vehicle entries and exits
CREATE TABLE vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number VARCHAR(20) UNIQUE NOT NULL,
    entry_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    exit_time TIMESTAMP NULL,
    fee DECIMAL(10,2) DEFAULT 0.00,
    slot_number INT,
    FOREIGN KEY (slot_number) REFERENCES parking_slots(slot_number)
);
Select*from vehicles;
-- Insert initial parking slots (example: 10 slots)
INSERT INTO parking_slots (status) VALUES 
('Available'), ('Available'), ('Available'), ('Available'), ('Available'),
('Available'), ('Available'), ('Available'), ('Available'), ('Available');

-- Trigger to update slot status when a vehicle enters
DELIMITER //
CREATE TRIGGER before_vehicle_insert
BEFORE INSERT ON vehicles
FOR EACH ROW
BEGIN
    DECLARE available_slot INT;
    
    -- Find an available slot
    SELECT slot_number INTO available_slot 
    FROM parking_slots 
    WHERE status = 'Available' 
    LIMIT 1;
    
    IF available_slot IS NOT NULL THEN
        -- Assign the slot to the vehicle
        SET NEW.slot_number = available_slot;
        
        -- Update the slot status to 'Occupied'
        UPDATE parking_slots 
        SET status = 'Occupied' 
        WHERE slot_number = available_slot;
    ELSE
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'No available parking slots!';
    END IF;
END //
DELIMITER ;

-- Trigger to update slot status when a vehicle exits
DELIMITER //
CREATE TRIGGER after_vehicle_update
AFTER UPDATE ON vehicles
FOR EACH ROW
BEGIN
    IF NEW.exit_time IS NOT NULL THEN
        -- Free up the assigned slot
        UPDATE parking_slots 
        SET status = 'Available' 
        WHERE slot_number = OLD.slot_number;
    END IF;
END //
DELIMITER ;
