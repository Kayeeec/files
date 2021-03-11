package org.vpreportcorrector.diagram.edit

import javafx.embed.swing.SwingNode
import javafx.event.EventHandler
import javafx.scene.control.ToggleButton
import javafx.scene.control.Tooltip
import javafx.scene.layout.Priority
import org.icepdf.ri.common.MyAnnotationCallback
import org.icepdf.ri.common.SwingViewBuilder
import org.icepdf.ri.util.FontPropertiesManager
import org.icepdf.ri.util.ViewerPropertiesManager
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.javafx.FontIcon
import org.vpreportcorrector.app.Styles
import org.vpreportcorrector.components.form.loadingOverlay
import org.vpreportcorrector.diagram.DiagramController
import org.vpreportcorrector.diagram.components.DiagramErrorsDrawerView
import org.vpreportcorrector.diagram.components.CustomSwingNode
import tornadofx.*
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.SwingUtilities

const val DEFAULT_PDF_VIEWER_ICON_SIZE = "_24"

// this is tornadofx view, that is embedded in a tab of a tabpane
class DiagramEditorView : View() {
    private val controller: DiagramController by inject()
    var diagramErrorsBtn: ToggleButton by singleAssign()
    private val diagramErrorsDrawer = find<DiagramErrorsDrawerView>(scope)

    private var swingController = controller.swingController
    private var viewerPanel: JComponent by singleAssign()
    private val swingNode = CustomSwingNode()

    private val centerView = vbox {
        fitToParentSize()
        widthProperty().onChange {
            resizeViewerPanel()
        }
        heightProperty().onChange {
            resizeViewerPanel()
        }
    }

    private var toolBar = hbox {
        style {
            padding = box(4.px)
        }
        hgrow = Priority.ALWAYS
    }

    init {
        createViewerAndOpenDocument()
        controller.loadData()
        
        // the free text annotation did not work without this - could not enter text in it
        swingNode.onMouseEntered = EventHandler {
            if (!swingNode.isFocused) {
                swingNode.requestFocus()
            }
        }
    }

    private fun resizeViewerPanel(){
        val width = centerView.widthProperty().value
        val height = centerView.heightProperty().value
        val dimension = Dimension(width.toInt(), height.toInt())
        swingNode.resize(width, height)
        SwingUtilities.invokeLater {
            viewerPanel.size = dimension
            viewerPanel.minimumSize = dimension
            viewerPanel.preferredSize = dimension
            viewerPanel.maximumSize = dimension
            viewerPanel.repaint()
        }
    }

    // this method creates the icepdf viewer
    private fun createViewerAndOpenDocument() {
        controller.model.loadingLatch.startLoading()
        try {
            SwingUtilities.invokeAndWait {
                swingController.setIsEmbeddedComponent(true)
                FontPropertiesManager.getInstance().loadOrReadSystemFonts()

                val properties = ViewerPropertiesManager.getInstance()
                setPdfViewerPreferences(properties)

                swingController.documentViewController.annotationCallback =
                    MyAnnotationCallback(swingController.documentViewController)
                val factory = SwingViewBuilder(swingController, properties)
                viewerPanel = factory.buildUtilityAndDocumentSplitPane(false)
                buildToolbar(factory)
                viewerPanel.revalidate()
                swingNode.content = viewerPanel
                centerView.add(swingNode)

                // open document
                swingController.openDocument(controller.model.path.toAbsolutePath().toString())
                viewerPanel.revalidate()
            }
        } catch (e: Exception) {
            log.severe(e.stackTraceToString())
        } finally {
            controller.model.loadingLatch.endLoading()
        }
    }

    private fun buildToolbar(factory: SwingViewBuilder) {
        with(toolBar) {
            flowpane {
                add(toSwingNode(factory.buildShowHideUtilityPaneButton()))
                button("", FontIcon(FontAwesomeSolid.SAVE)) {
                    addClass(Styles.flatButton)
                    tooltip = Tooltip("Save")
                    action {
                        controller.save()
                    }
                }
                add(toSwingNode(factory.buildFitPageButton()))
                add(toSwingNode(factory.buildPanToolButton()))
                add(toSwingNode(factory.buildTextSelectToolButton()))
                separator()
                add(toSwingNode(factory.buildSelectToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
                add(toSwingNode(factory.buildLineAnnotationToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
                add(toSwingNode(factory.buildLineArrowAnnotationToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
                add(toSwingNode(factory.buildSquareAnnotationToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
                add(toSwingNode(factory.buildCircleAnnotationToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
                add(toSwingNode(factory.buildInkAnnotationToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
                add(toSwingNode(factory.buildFreeTextAnnotationToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
                add(toSwingNode(factory.buildTextAnnotationToolButton(DEFAULT_PDF_VIEWER_ICON_SIZE)))
            }

            hbox { hgrow = Priority.ALWAYS }

            diagramErrorsBtn = togglebutton("") {
                addClass(Styles.flatButton)
                isSelected = false
                graphic = FontIcon(FontAwesomeSolid.TAGS)
                tooltip = Tooltip("Show errors the diagram contains")
            }
        }
    }

    private fun toSwingNode(jComponent: JComponent): SwingNode {
        val sn = SwingNode()
        sn.content = jComponent
        return sn
    }

    private fun setPdfViewerPreferences(properties: ViewerPropertiesManager) {
        properties.clearPreferences()
        properties.setFloat(ViewerPropertiesManager.PROPERTY_DEFAULT_ZOOM_LEVEL, 1.25f)
        properties.set(ViewerPropertiesManager.PROPERTY_ICON_DEFAULT_SIZE, DEFAULT_PDF_VIEWER_ICON_SIZE)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ZOOM, false)

        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_OPEN, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_SAVE, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_PRINT, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_UPANE, true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_SEARCH, false)

        // only show annotations card and layers in utility pane
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_SEARCH, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_ANNOTATION, true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_ANNOTATION_DESTINATIONS, true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_ANNOTATION_FLAGS, true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_ATTACHMENTS, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_BOOKMARKS, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_SIGNATURES, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_THUMBNAILS, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITYPANE_LAYERS, true)

        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_STATUSBAR, false)

        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_FIT, true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ROTATE, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_TOOL, true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_FORMS, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_SEARCH, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_FULL_SCREEN, false)

        // enable only certain quick annotation buttons
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION, true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_ARROW     , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_CIRCLE    , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_FREE_TEXT , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_HIGHLIGHT , false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_INK       , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_LINE      , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_LINK      , false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_PERMISSION, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_PREVIEW   , false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_RECTANGLE , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_SELECTION , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_STRIKE_OUT, false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_TEXT      , true)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_UNDERLINE , false)
        properties.setBoolean(ViewerPropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION_UTILITY   , false)
    }

    override val root = stackpane {
        fitToParentSize()
        addClass(Styles.diagramAnnotatorView)
        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        borderpane {
            fitToParentSize()
            top = toolBar
            center = centerView
            centerView.fitToParentSize()
            right = diagramErrorsDrawer.root
            diagramErrorsDrawer.drawerExpandedProperty.bind(diagramErrorsBtn.selectedProperty())
        }

        loadingOverlay {
            visibleWhen { controller.model.loadingLatch.isLoading }
        }
    }
}
