package com.intenovation.appfw.ui;

import com.intenovation.appfw.config.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Swing implementation of the UIService interface
 */
public class SwingUIService implements UIService {
    private static final Logger LOGGER = Logger.getLogger(SwingUIService.class.getName());

    @Override
    public boolean showConfigDialog(String title, ConfigurationDefinition config) {
        // Create a map to hold UI components
        Map<String, Component> components = new HashMap<>();
        
        // Create panel for the form with a grid layout
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Constraints for the grid
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 5, 5, 10);
        
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(5, 0, 5, 5);
        
        // Add components for each config item
        for (ConfigItem item : config.getConfigItems()) {
            // Add label
            JLabel label = new JLabel(item.getDisplayName() + ":");
            formPanel.add(label, labelConstraints);
            labelConstraints.gridy++;
            
            // Add appropriate component based on type
            Component component = createComponentForItem(item);
            formPanel.add(component, fieldConstraints);
            fieldConstraints.gridy++;
            
            // Store component for later retrieval
            components.put(item.getKey(), component);
        }
        
        // Show dialog
        int option = JOptionPane.showConfirmDialog(null, formPanel, title, 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            // Collect values from components
            Map<String, Object> values = new HashMap<>();
            
            for (ConfigItem item : config.getConfigItems()) {
                Component component = components.get(item.getKey());
                Object value = getComponentValue(component, item);
                values.put(item.getKey(), value);
            }
            
            // Apply configuration
            config.applyConfiguration(values);
            return true;
        }
        
        return false;
    }
    
    /**
     * Create a UI component for a configuration item
     */
    private Component createComponentForItem(ConfigItem item) {
        Map<String, Object> currentValues = new HashMap<>(); // Default to empty map
        
        // If item is part of a ConfigurationDefinition, get current values
        if (item instanceof ConfigurationDefinition) {
            currentValues = ((ConfigurationDefinition) item).getCurrentValues();
        }
        
        Object currentValue = currentValues.get(item.getKey());
        if (currentValue == null) {
            currentValue = item.getDefaultValue();
        }
        
        switch (item.getType()) {
            case TEXT:
                JTextField textField = new JTextField();
                if (currentValue != null) {
                    textField.setText(currentValue.toString());
                }
                return textField;
                
            case PASSWORD:
                JPasswordField passwordField = new JPasswordField();
                if (currentValue != null) {
                    passwordField.setText(currentValue.toString());
                }
                return passwordField;
                
            case CHECKBOX:
                JCheckBox checkBox = new JCheckBox();
                if (currentValue instanceof Boolean) {
                    checkBox.setSelected((Boolean) currentValue);
                }
                return checkBox;
                
            case DIRECTORY:
                JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
                JTextField dirField = new JTextField();
                if (currentValue instanceof File) {
                    dirField.setText(((File) currentValue).getAbsolutePath());
                } else if (currentValue != null) {
                    dirField.setText(currentValue.toString());
                }
                
                JButton browseBtn = new JButton("Browse...");
                browseBtn.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    
                    // Set initial directory if one is specified
                    String currentDir = dirField.getText();
                    if (currentDir != null && !currentDir.isEmpty()) {
                        File dir = new File(currentDir);
                        if (dir.exists()) {
                            chooser.setCurrentDirectory(dir);
                        }
                    }
                    
                    if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {
                        dirField.setText(chooser.getSelectedFile().getAbsolutePath());
                    }
                });
                
                dirPanel.add(dirField, BorderLayout.CENTER);
                dirPanel.add(browseBtn, BorderLayout.EAST);
                return dirPanel;
                
            case NUMBER:
                JTextField numField = new JTextField();
                if (currentValue != null) {
                    numField.setText(currentValue.toString());
                }
                return numField;
                
            case DROPDOWN:
                if (item instanceof DropdownConfigItem) {
                    JComboBox<String> comboBox = new JComboBox<>();
                    DropdownConfigItem dropdownItem = (DropdownConfigItem) item;
                    
                    for (String option : dropdownItem.getOptions()) {
                        comboBox.addItem(option);
                    }
                    
                    if (currentValue != null) {
                        comboBox.setSelectedItem(currentValue.toString());
                    }
                    
                    return comboBox;
                }
                return new JComboBox<>();
                
            default:
                return new JTextField();
        }
    }
    
    /**
     * Get value from a UI component
     */
    private Object getComponentValue(Component component, ConfigItem item) {
        switch (item.getType()) {
            case TEXT:
                return ((JTextField) component).getText();
                
            case PASSWORD:
                return new String(((JPasswordField) component).getPassword());
                
            case CHECKBOX:
                return ((JCheckBox) component).isSelected();
                
            case DIRECTORY:
                // For directory panels, get the text field value
                if (component instanceof JPanel) {
                    JPanel panel = (JPanel) component;
                    Component[] comps = panel.getComponents();
                    for (Component comp : comps) {
                        if (comp instanceof JTextField) {
                            return new File(((JTextField) comp).getText());
                        }
                    }
                }
                return null;
                
            case NUMBER:
                try {
                    String text = ((JTextField) component).getText();
                    if (text.contains(".")) {
                        return Double.parseDouble(text);
                    } else {
                        return Integer.parseInt(text);
                    }
                } catch (NumberFormatException e) {
                    return 0;
                }
                
            case DROPDOWN:
                return ((JComboBox<?>) component).getSelectedItem();
                
            default:
                return null;
        }
    }

    @Override
    public void showInfo(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void showError(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void showWarning(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
        });
    }

    @Override
    public boolean openDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }
        
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(directory);
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening directory with Desktop API", e);
        }
        
        // Fallback to command line for specific operating systems
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("windows")) {
                Runtime.getRuntime().exec("explorer.exe \"" + directory.getAbsolutePath() + "\"");
                return true;
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", directory.getAbsolutePath()});
                return true;
            } else if (os.contains("linux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", directory.getAbsolutePath()});
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening directory with command line", e);
        }
        
        return false;
    }
}