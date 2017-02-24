(function() {
	var STR_ADAPTIVE_MEDIA_RETURN_TYPE = 'com.liferay.adaptive.media.image.item.selector.ImageAdaptiveMediaURLItemSelectorReturnType';

	CKEDITOR.plugins.add(
		'adaptivemedia',
		{
			init: function(editor) {
				var instance = this;

				instance._bindEvent(editor);
			},

			_bindEvent: function(editor) {
				var instance = this;

				editor.on(
					'beforeCommandExec',
					function(event) {
						if (event.data.name === 'imageselector') {
							event.removeListener();

							event.cancel();

							var onSelectedImageChangeFn = AUI._.bind(
								instance._onSelectedImageChange,
								instance,
								editor
							);

							editor.execCommand('imageselector', onSelectedImageChangeFn);

							instance._bindEvent(editor);
						}
					}
				);

				editor.on(
					'imageUploaded',
					function(event) {
						var fileEntryId = event.data.fileEntryId;
						var imageNode = event.data.el;
						var returnType = event.data.uploadImageReturnType;

						if (returnType === STR_ADAPTIVE_MEDIA_RETURN_TYPE) {
							imageNode.attr('data-fileEntryId', fileEntryId);
						}
					}
				);
			},

			_onSelectedImageChange: function(editor, imageSrc, selectedItem) {
				var img = CKEDITOR.dom.element.createFromHtml('<img>');

				if (selectedItem.returnType === STR_ADAPTIVE_MEDIA_RETURN_TYPE) {
					var itemValue = JSON.parse(selectedItem.value);

					img.setAttribute('src', itemValue.url);
					img.setAttribute('data-fileEntryId', itemValue.fileEntryId);
				}
				else {
					img.setAttribute('src', imageSrc);
				}

				editor.insertElement(img);

				editor.setData(editor.getData());
			}
		}
	);
})();