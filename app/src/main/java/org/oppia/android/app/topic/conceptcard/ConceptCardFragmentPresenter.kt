package org.oppia.android.app.topic.conceptcard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.oppia.android.R
import org.oppia.android.app.fragment.FragmentScope
import org.oppia.android.app.model.ProfileId
import org.oppia.android.app.translation.AppLanguageResourceHandler
import org.oppia.android.databinding.ConceptCardFragmentBinding
import org.oppia.android.domain.oppialogger.OppiaLogger
import org.oppia.android.domain.oppialogger.analytics.AnalyticsController
import org.oppia.android.domain.translation.TranslationController
import org.oppia.android.util.gcsresource.DefaultResourceBucketName
import org.oppia.android.util.parser.html.ConceptCardHtmlParserEntityType
import org.oppia.android.util.parser.html.HtmlParser
import javax.inject.Inject

/** Presenter for [ConceptCardFragment], sets up bindings from ViewModel. */
@FragmentScope
class ConceptCardFragmentPresenter @Inject constructor(
  private val fragment: Fragment,
  private val oppiaLogger: OppiaLogger,
  private val analyticsController: AnalyticsController,
  private val htmlParserFactory: HtmlParser.Factory,
  @ConceptCardHtmlParserEntityType private val entityType: String,
  @DefaultResourceBucketName private val resourceBucketName: String,
  private val conceptCardViewModel: ConceptCardViewModel,
  private val translationController: TranslationController,
  private val appLanguageResourceHandler: AppLanguageResourceHandler
) : HtmlParser.CustomOppiaTagActionListener {
  private lateinit var profileId: ProfileId

  /**
   * Sets up data binding and toolbar.
   * Host activity must inherit ConceptCardListener to dismiss this fragment.
   */
  fun handleCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    skillId: String,
    profileId: ProfileId
  ): View? {
    this.profileId = profileId
    val binding = ConceptCardFragmentBinding.inflate(
      inflater,
      container,
      /* attachToRoot= */ false
    )
    val view = binding.conceptCardExplanationText

    conceptCardViewModel.initialize(skillId, profileId)
    logConceptCardEvent(skillId)

    binding.conceptCardToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
    binding.conceptCardToolbar.setNavigationContentDescription(
      R.string.navigate_up
    )
    binding.conceptCardToolbar.setNavigationOnClickListener {
      (fragment.requireActivity() as? ConceptCardListener)?.dismissConceptCard()
    }

    binding.let {
      it.viewModel = conceptCardViewModel
      it.lifecycleOwner = fragment
    }

    conceptCardViewModel.conceptCardLiveData.observe(
      fragment
    ) { ephemeralConceptCard ->
      val explanationHtml =
        translationController.extractString(
          ephemeralConceptCard.conceptCard.explanation,
          ephemeralConceptCard.writtenTranslationContext
        )
      view.text =
        htmlParserFactory.create(
          resourceBucketName,
          entityType,
          skillId,
          customOppiaTagActionListener = this,
          imageCenterAlign = true,
          displayLocale = appLanguageResourceHandler.getDisplayLocale()
        ).parseOppiaHtml(
          explanationHtml,
          view,
          supportsLinks = true,
          supportsConceptCards = true
        )
    }

    return binding.root
  }

  private fun logConceptCardEvent(skillId: String) {
    analyticsController.logImportantEvent(
      oppiaLogger.createOpenConceptCardContext(skillId), profileId
    )
  }

  override fun onConceptCardLinkClicked(view: View, skillId: String) {
    ConceptCardFragment
      .bringToFrontOrCreateIfNew(skillId, profileId, fragment.parentFragmentManager)
  }
}
