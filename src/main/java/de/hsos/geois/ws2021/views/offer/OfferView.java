package de.hsos.geois.ws2021.views.offer;

import java.util.ArrayList;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import de.hsos.geois.ws2021.data.entity.Customer;
import de.hsos.geois.ws2021.data.entity.Offer;
import de.hsos.geois.ws2021.data.entity.OfferPosition;
import de.hsos.geois.ws2021.data.service.CustomerDataService;
import de.hsos.geois.ws2021.data.service.OfferDataService;
import de.hsos.geois.ws2021.data.service.OfferPositionDataService;
import de.hsos.geois.ws2021.views.MainView;

@Route(value = "offer", layout = MainView.class)
@PageTitle("Offer")
@CssImport("./styles/views/mydevicemanager/my-device-manager-view.css")
@RouteAlias(value = "offer", layout = MainView.class)
public class OfferView extends Div {

    private static final long serialVersionUID = 4740201357551960590L;

    private Grid<Offer> grid;

    private TextField companyName = new TextField();
    private TextField customerAddress = new TextField();
    private TextField customerEmail = new TextField();
    private TextField offNr = new TextField();
    private TextField customerPhone = new TextField();
    private Label gridHeader = new Label("Offerpositions");
    
    private ComboBox<Customer> customer = new ComboBox<Customer>();
    
    private Grid<OfferPosition> offerPositionGrid = new Grid<OfferPosition>();

    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");
    private Button order = new Button("Convert to Order");

    private Binder<Offer> binder;

    private Offer currentOffer = new Offer();

    private OfferDataService offerService;

    private boolean givenObject = false;

    public OfferView() {
        setId("my-device-manager-view");
        this.offerService = OfferDataService.getInstance();
        // Configure Grid
        grid = new Grid<>(Offer.class);
        grid.setColumns("offNr","customerFirstName", "customerLastName", "companyName", "customerAddress");
        grid.setDataProvider(new OfferDataProvider());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                Offer offerFromBackend = offerService.getById(event.getValue().getId());
                // when a row is selected but the data is no longer available, refresh grid
                if (offerFromBackend != null) {
                    populateForm(offerFromBackend	);
                } else {
                    refreshGrid();
                }
            } else {
                clearForm();
            }
        });

        // Configure Form
        binder = new Binder<>(Offer.class);

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });
        
        order.addClickListener(e -> {
        	Notification.show("Offer will be convertet to an Order now!");
        });
        

        save.addClickListener(e -> {
            try {   
                givenObject = this.currentOffer.getId() != null ? true : false;

                binder.writeBean(this.currentOffer);
                //binding those objects creates and saves the object as well
                OfferDataService.getInstance().save(this.currentOffer);
                this.connectWithCustomer();
                clearForm();
                refreshGrid();
                Notification.show("Offer details stored.");
            } catch (ValidationException validationException) {
                Notification.show("An exception happened while trying to store the offer details.");
            }
            catch(NullPointerException npE) {
            	Notification.show("Offer could not be saved. Please choose a Customer!");
            }
        });
        
        // add customers to combobox customers
        customer.setItems(CustomerDataService.getInstance().getAll());
        
        customer.addValueChangeListener(event -> {
        	if (event.isFromClient() && event.getValue()!=null) {
                if (this.currentOffer == null) { this.currentOffer = new Offer(); }
        		this.currentOffer.setCustomer(event.getValue());
                
                //fields automatically filled
                companyName.setValue(currentOffer.getCustomer().getCompanyName());
                customerAddress.setValue(currentOffer.getCustomer().getStreetAndNr() + ", " + currentOffer.getCustomer().getZipCode() + " " + currentOffer.getCustomer().getPlace());
                customerEmail.setValue(currentOffer.getCustomer().getEmail());
                customerPhone.setValue(currentOffer.getCustomer().getPhone());
                currentOffer.setCustomerFirstName(currentOffer.getCustomer().getFirstName());
                currentOffer.setCustomerLastName(currentOffer.getCustomer().getLastName());
        	}
        });
        
        SplitLayout splitLayout = new SplitLayout();
        SplitLayout offerPositionLayout = new SplitLayout();
        splitLayout.setSizeFull();
       
        offerPositionLayout.setOrientation(Orientation.VERTICAL);

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setId("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setId("editor");
        editorLayoutDiv.add(editorDiv);
        
        customer.setRequired(true);
        customer.setErrorMessage("Please choose Customer!");
        companyName.setReadOnly(true);
        customerEmail.setReadOnly(true);
        customerAddress.setReadOnly(true);
        customerPhone.setReadOnly(true);

        FormLayout formLayout = new FormLayout();
        addFormItem(editorDiv, formLayout, offNr, "Offer Number");
        addFormItem(editorDiv, formLayout, customer, "Customer");
        addFormItem(editorDiv, formLayout, companyName, "Company Name");
        addFormItem(editorDiv, formLayout, customerEmail, "Customer Email");
        addFormItem(editorDiv, formLayout, customerAddress, "Customer Addresse");
        addFormItem(editorDiv, formLayout, customerPhone, "Customer Phone");
        
        createButtonLayout(editorLayoutDiv);
        
     // add grid
        offerPositionGrid.addColumn(OfferPosition::getDeviceTyp).setHeader("Device Typ");
        offerPositionGrid.addColumn(OfferPosition::getQuantity).setHeader("Quantity");
        offerPositionGrid.addColumn(OfferPosition::getPrice).setHeader("Price");
        offerPositionGrid.addColumn(OfferPosition::getTotalPrice).setHeader("Total Price");
        offerPositionGrid.addColumn(
        	    new NativeButtonRenderer<>("Remove",
        	       clickedOfferPosition -> {
        	           this.currentOffer.removeOfferPosition(clickedOfferPosition);
        	           clickedOfferPosition.setOffer(null);
					   // persist customer
        	           try {
							binder.writeBean(this.currentOffer);
							this.currentOffer = offerService.update(this.currentOffer);
						} catch (ValidationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					   // persist clickedDevice
        	           OfferPositionDataService.getInstance().save(clickedOfferPosition);
        	           populateForm(this.currentOffer);
        	    })
        	);
        
        offerPositionGrid.setWidthFull();
        formLayout.add(gridHeader, offerPositionGrid);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setId("button-layout");
        buttonLayout.setWidthFull();
        buttonLayout.setSpacing(true);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        order.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel, order);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setId("grid-wrapper");
        wrapper.setWidthFull();
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void addFormItem(Div wrapper, FormLayout formLayout, AbstractField field, String fieldName) {
        formLayout.addFormItem(field, fieldName);
        wrapper.add(formLayout);
        field.getElement().getClassList().add("full-width");
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Offer value) {
        this.currentOffer= value;
        binder.readBean(this.currentOffer);
        if (currentOffer!=null) {
    		binder.bindInstanceFields(this);
	        offerPositionGrid.setItems(this.currentOffer.getOfferpositions());
    	} else {
    		offerPositionGrid.setItems(new ArrayList<OfferPosition>());
    	}
    }

    private void connectWithCustomer() {
        if (givenObject) { this.currentOffer.getCustomer().removeOffer(this.currentOffer); }
        boolean ok = this.currentOffer.getCustomer().addOffer(this.currentOffer);
        if (ok) { CustomerDataService.getInstance().update(this.currentOffer.getCustomer()); }
    }
}
