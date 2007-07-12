/**
 ** jspwiki preferences support routines
 ** based on brushedGroup.js
 ** Dirk Frederickx Jun 07
 ** Uses mootools library
 **/

var WikiGroup =
{
	MembersID   : "membersfield",
	GroupTltID  : "grouptemplate",
	GroupID     : "groupfield",
	NewGroupID  : "newgroup",
	GroupInfoID : "groupinfo",
	CancelBtnID : "cancelButton",
	SaveBtnID   : "saveButton",
	CreateBtnID : "createButton",
	DeleteBtnID : "deleteButton",
	groups      : { "(new)": { members:"", groupInfo:"" } },
	cursor      : null,
	isEditOn    : false,
	isCreateOn  : false,

	putGroup: function(group, members, groupInfo, isSelected){
		this.groups[ group ] = { members: members, groupInfo: groupInfo };

		var g = $(this.GroupTltID);
		var gg = g.clone().setHTML(group);
		gg.id = '';
		g.parentNode.appendChild(gg);
		$(gg).show();

		if(isSelected || !this.cursor) this.onMouseOverGroup(gg);
	} ,

	onMouseOverGroup: function(node){
		if(this.isEditOn) return;
		this.setCursor(node);

		var g = this.groups[ (node.id == this.GroupID) ? "(new)": node.innerHTML ];
		$(this.MembersID).value = g.members;
		$(this.GroupInfoID).innerHTML = g.groupInfo;
	} ,

	setCursor: function(node){
		if(this.cursor) $(this.cursor).removeClass('cursor');
		this.cursor = $(node).addClass('cursor');
	} ,

	//create new group: focus on input field
	onClickNew: function(){
		if(this.isEditOn) return;

		this.isCreateOn = true;
		$(this.MembersID).value = "";
		this.toggle();
	} ,

	//toggle edit status of Group Editor
	toggle: function(){
		this.isEditOn = !this.isEditOn; //toggle

		$(this.MembersID  ).disabled =
		$(this.SaveBtnID  ).disabled =
		$(this.CreateBtnID).disabled =
		$(this.CancelBtnID).disabled = !this.isEditOn;
		var del = $(this.DeleteBtnID);
		if(del) del.disabled = this.isCreateOn || !this.isEditOn;

		if(this.isCreateOn) { $(this.CreateBtnID).toggle(); $(this.SaveBtnID).toggle() };

		var newGrp  = $(this.NewGroupID),
			members = $(this.MembersID);

		if(this.isEditOn){
			members.getParent().addClass("cursor");

			newGrp.disabled = !this.isCreateOn;
			if(this.isCreateOn) { newGrp.focus(); } else { members.focus(); }
		} else {
			members.getParent().removeClass("cursor");

			if(this.isCreateOn){
				this.isCreateOn = false;
				newGrp.value = newGrp.defaultValue;
				members.value = "";
			}
			newGrp.blur();
			members.blur();
			newGrp.disabled = false;
		}
	} ,

	// submit form to create new group
	onSubmitNew: function(form, actionURL){
		var newGrp = $(this.NewGroupID);
		if(newGrp.value == newGrp.defaultValue){
			alert("group.validName".localize());
			newGrp.focus();
		} else this.onSubmit(form, actionURL);
	} ,

	// submit form: fill out actual group and members info
	onSubmit: function(form, actionURL){
		if(! this.cursor) return false;
		var g = (this.cursor.id == this.GroupID) ? $(this.NewGroupID).value: this.cursor.innerHTML;

		/* form.action = actionURL; -- doesn't work in IE */
		form.setAttribute("action", actionURL) ;
		form.group.value = g;
		form.members.value = $(this.MembersID).value;
		form.action.value = "save";

		Wiki.submitOnce(form);
		form.submit();
	}
}