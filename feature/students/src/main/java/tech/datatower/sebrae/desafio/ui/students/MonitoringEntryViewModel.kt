package tech.datatower.sebrae.desafio.ui.students

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.BehaviorRecord
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUp
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeed
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.data.model.PsychologicalNeed
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringSnapshot
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MonitoringEntryViewModel
@Inject
constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val studentId: Int = checkNotNull(savedStateHandle[AppRoutes.STUDENT_ID_ARG])

  val snapshot: StateFlow<StudentMonitoringSnapshot?> =
      repository
          .observeStudentMonitoringSnapshot(studentId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  suspend fun saveEntry(
      entryType: MonitoringEntryType,
      // Attendance
      attendanceStatus: AttendanceStatus = AttendanceStatus.Present,
      minutesLate: Int = 0,
      justification: String? = null,
      // Behavior
      participationScore: Int = 3,
      deliveryStatus: ActivityDeliveryStatus = ActivityDeliveryStatus.OnTime,
      delayMinutes: Int = 0,
      grade: Float? = null,
      behaviorNote: String = "",
      // Pedagogical
      pedagogicalType: PedagogicalNeedType = PedagogicalNeedType.Report,
      pedagogicalDescription: String = "",
      pedagogicalAccommodations: List<String> = emptyList(),
      // Psychological
      psychologicalSummary: String = "",
      confidentiality: ConfidentialityLevel = ConfidentialityLevel.Restricted,
      nextStep: String = "",
      // Parent follow-up
      parentChannel: ParentContactChannel = ParentContactChannel.Meeting,
      parentOutcome: ParentFollowUpStatus = ParentFollowUpStatus.Pending,
      parentResponsible: String = "",
      parentNotes: String = "",
  ): Boolean {
    val companyId = AuthManager.currentCompany.value?.id ?: return false
    val today = LocalDate.now()

    return try {
      when (entryType) {
        MonitoringEntryType.ATTENDANCE -> {
          repository.insertAttendanceRecord(
              companyId,
              studentId,
              AttendanceRecord(
                  date = today,
                  status = attendanceStatus,
                  minutesLate = minutesLate,
                  justification = justification,
              ),
          )
        }
        MonitoringEntryType.BEHAVIOR -> {
          repository.insertBehaviorRecord(
              companyId,
              studentId,
              BehaviorRecord(
                  date = today,
                  participationScore = participationScore,
                  activityDelivery = deliveryStatus,
                  delayMinutes = delayMinutes,
                  grade = grade,
                  note = behaviorNote,
              ),
          )
        }
        MonitoringEntryType.PEDAGOGICAL -> {
          repository.insertPedagogicalNeedRecord(
              companyId,
              studentId,
              PedagogicalNeed(
                  type = pedagogicalType,
                  description = pedagogicalDescription,
                  expiresAt = null,
                  accommodations = pedagogicalAccommodations,
              ),
          )
        }
        MonitoringEntryType.PSYCHOLOGICAL -> {
          repository.insertPsychologicalNeedRecord(
              companyId,
              studentId,
              PsychologicalNeed(
                  summary = psychologicalSummary,
                  confidentiality = confidentiality,
                  nextStep = nextStep,
                  reviewAt = today.plusMonths(1),
              ),
          )
        }
        MonitoringEntryType.PARENT_FOLLOWUP -> {
          repository.insertParentFollowUpRecord(
              companyId,
              studentId,
              ParentFollowUp(
                  date = today,
                  channel = parentChannel,
                  outcome = parentOutcome,
                  responsible = parentResponsible,
                  notes = parentNotes,
              ),
          )
        }
      }
      true
    } catch (_: Exception) {
      false
    }
  }
}
