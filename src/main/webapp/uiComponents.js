window.uiC = {};

var ui = {
	loadSelectDM: function(){
		//limpar select
		var select = document.getElementById("idDecisions");
		while (select.firstChild) {
		    select.removeChild(select.firstChild);
		}
		
		if(document.getElementById("checkboxDM").checked){
			document.getElementById("idDecisions").style.display = "block";
			var elem = ui.currentElement.collection.models;
			
			for(var i = 0; i < elem.length; i++){
				var lab = elem[i].attr('text/text');
				var isElGoal = elem[i].attributes.type.toUpperCase().includes("GOAL");
				var isElTask = elem[i].attributes.type.toUpperCase().includes("TASK");
				var labelPattern = ui.currentElement.attr('text/text');
				
				if(lab != labelPattern){
					if(lab && (isElTask || isElGoal)){
		        		var div = document.createElement('section'); 
						div.className = "linha"
						var checkbox = document.createElement("input");
			            checkbox.type = "checkbox"; 
			            checkbox.value = lab; 
			            checkbox.name = lab + "_" + i; 
	
						var tam = lab.indexOf(":");
						if(tam > 0){
							lab = lab.substring(0, tam);
						}
						/*if(labelPattern.includes(lab)){
							checkbox.checked = true;
						}*/
			              
			            // creating label for checkbox 
			            var label = document.createElement('label'); 
			            label.for = lab + "_" + i; 
						label.textContent = (isElGoal ? " GOAL - " + lab : " TASK - " + lab); 
		
						select.appendChild(div);
						div.appendChild(checkbox);
						div.appendChild(label);
					}
				}
			}
		}else{
			select.style.display = "none";
		}
	}
};

uiC.ButtonModel = Backbone.Model.extend({
	defaults: {
		name: '',
		label: '',
		tooltip: '',
		statusText: '',
		precondition: function() { return true; },
		action: 'view',
		active: false
	},
	act: function() {
		this.set('active', true);
		ui.currentState = this.get('action');
		ui.currentAddingElement = this.get('name');
		ui.currentButton = this;
		if (ui.currentState === 'addActor') {
			$('#diagram').css('cursor', 'crosshair');
			$('#diagram g').css('cursor', 'no-drop');
			$('#diagram .actorKindMain').css('cursor', 'no-drop');
		}
		else {
			$('#diagram').css('cursor', 'no-drop');
			$('#diagram g').css('cursor', 'crosshair');
			$('#diagram .actorKindMain').css('cursor', 'crosshair');
		}
	},
	end: function() {
		this.set('active', false);
		//resets the values of the app variables

		ui.currentState = 'view';
		ui.currentAddingElement = 'none';
		if (ui.linkSource && ui.linkSource.unhighlight) ui.linkSource.unhighlight();
		ui.resetLinkSource();
		ui.resetLinkTarget();
		ui.currentButton = null;
		ui.changeStatus('');
		ui.resetPointerStyles();
	}

});

uiC.ButtonView = Backbone.View.extend({
	tagName: 'span',
	className: 'addButton',
	template: _.template($('#addButtonTemplate').html()),

	events: {
		'mousedown button': 'buttonClickHandler'//meaning: when its button is clicked, the buttonClickHandler is called
	},

	initialize: function() {
		if (!this.model.get('name')) {
			this.model.set('name', this.model.get('label'));
		}
		this.listenTo(this.model, 'change:active', this.highlight);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
		$('#addToolbarButtons').append(this.$el);
		return this;
	},

	buttonClickHandler: function(event) {
		if (ui.currentButton) {
			ui.currentButton.end();
		}
		if (this.model.get('precondition')()) {
			if (this.model.get('name') === 'DependencyLink') {
				var newType = window.prompt('Select type:\n  g for goal;\n  s for softgoal;\n  t for task;\n  r for resource.', 'g');
				if (newType !== null) {
					ui.dependencyType = newType;
				}
			}
			this.model.act();
			ui.changeStatus(this.model.get('statusText'));
		}
	},

	highlight: function(element) {
		this.$('button').toggleClass('buttonHighlight', element.get('active'));
		this.$('button').blur();
	}

});

uiC.DropdownItemView = Backbone.View.extend({
	tagName: 'li',
	template: _.template($('#addDropdownButtonTemplate').html()),

	events: {
		'mousedown': 'buttonClickHandler'//meaning: when its button is clicked, the buttonClickHandler is called
	},

	initialize: function() {
		if (!this.model.get('name')) {
			this.model.set('name', this.model.get('label'));
		}
		this.listenTo(this.model, 'change:active', this.highlight);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
		$(this.attributes.parent).append(this.$el);
		return this;
	},

	buttonClickHandler: function(event) {
		if (ui.currentButton) {
			ui.currentButton.end();
		}
		ui.dependencyType = this.model.get('name');
		if (this.model.get('precondition')()) {

			this.model.act();
			ui.changeStatus(this.model.get('statusText'));
		}
	},

	highlight: function(element) {
		this.$('button').toggleClass('buttonHighlight', element.get('active'));
	}

});


uiC.CellTableView = Backbone.View.extend({
	template: _.template($('#propertyTemplate').html()),

	initialize: function() {
		this.model.on('change', this.render, this);
	},

	render: function() {
		$('#propertyTable tbody').html(this.template({ propertyName: 'Text', propertyValue: this.model.attr('text/text').replace(/(\r\n|\n|\r)/gm, ' '), options: 'teste' }));
		$('#propertyTable a').editable({
			success: function(response, newValue) {
				if (newValue) {
					ui.currentElement.changeNodeContent(newValue);
				}
			}
		})
			.on('shown', ui.changeStateToEdit)
			.on('hidden', ui.changeStateToView);

		for (var propertyName in this.model.prop('customProperties')) {
			$('#propertyTable tbody').append(this.template({ 'propertyName': propertyName, 'propertyValue': this.model.prop('customProperties/' + propertyName), 'options': 'ds' }));
			$('#current' + propertyName).editable({
				success: function(response, newValue) {
					if (newValue) changeCustomPropertyValue(ui.currentElement, $(this).attr('data-name'), newValue); //update backbone model
				}
			}
			)
				.on('shown', ui.changeStateToEdit)
				.on('hidden', ui.changeStateToView);
		}
		$('#cellButtons').html('<button type="button" id="addPropertyButton" class="btn btn-primary">Add Property</button>');
		$('#addPropertyButton').click(function() {
			var modal = document.getElementById("modalRefact");
			var tablePropertyEditable = document.getElementById("tablePropertyEditable");
			var tableNameEditable = document.getElementById("tableNameEditable");
			tablePropertyEditable.style.display = "block";
			tableNameEditable.style.display = "none";
			modal.style.display = "block";
			document.getElementById("idDecisions").style.display = "none";
			
		var isGoal = ui.currentElement.attributes.type.toUpperCase().includes("GOAL");
			if(!isGoal){
				document.getElementById("checkboxList").style.display = "none";
			}
			
			if(ui.currentElement.prop('customProperties/' + "selected")){
				document.getElementById("checkboxRoot").checked = true;
			}
			
			var label = ui.currentElement.attr('text/text');
			if(label.includes("[DM")){
				document.getElementById("checkboxDM").checked = true;
				var select = document.getElementById("idDecisions");
				select.style.display = "block";
				ui.loadSelectDM();
			}
			
		/*	var newPropertyName = window.prompt('Name of the new custom property', 'newProperty');
			if (newPropertyName) {
				if (!ui.currentElement.prop('customProperties/' + newPropertyName)) {
					ui.currentElement.prop('customProperties/' + newPropertyName, '');
				}
				else {
					alert('ERROR: This property has been previously defined');
				}
			}*/
		});
		

		/*$('#removePropertyButton').click(function () {
            var newPropertyName = window.prompt('Name of the new custom property', 'newProperty');
            if (newPropertyName) {
                if (! ui.currentElement.prop('customProperties/' + newPropertyName) ) {
                    ui.currentElement.
                }
                else {
                    alert('ERROR: This property has been previously defined');
                }
            }
        });*/

		if (this.model.isKindOfActor()) {
			$('#cellButtons').append('<button type="button" class="ml-sm btn btn-primary" id="collapseButton">Collapse/Expand</button>');
			$('#collapseButton').click(function() {
				if (ui.currentElement) ui.currentElement.toggleCollapse();
			});
		}
		return this;
	},

	buttonClickHandler: function(event) {
		if (ui.currentButton) {
			ui.currentButton.end();
		}
		this.model.act();
	},

	highlight: function(element) {
		this.$('button').toggleClass('buttonHighlight', element.get('active'));
		//perhaps it's better to use 'changedAttributes', to prevent unnecessary updates
	}

});

$(document).ready(function() {
	$.fn.editable.defaults.mode = 'inline';//x-editable setting
});

function changeCustomPropertyValue(model, propertyName, propertyValue) {
	model.prop('customProperties/' + propertyName, propertyValue);
}
